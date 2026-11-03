"""Erzeugt GermanAreaCodes.kt aus dem Vorwahlverzeichnis der Bundesnetzagentur.

Aufruf aus dem Repo-Root:  python scripts/update_german_area_codes.py

Quelle: Bundesnetzagentur, Vorwahlverzeichnis (VwV), ZIP mit einer CSV-Datei
(https://www.bundesnetzagentur.de/DE/Fachthemen/Telekommunikation/Nummerierung/ONRufnr/Einteilung_ONB/start.html).
Lizenz: Datenlizenz Deutschland – Namensnennung – Version 4.0 (laut Impressum der Bundesnetzagentur für .csv-Dateien,
falls nicht anders gekennzeichnet). Übernommen werden die aktiven Ortsnetzkennzahlen mit ihrem Ortsnetznamen.
"""

import csv
import io
import re
import urllib.request
import zipfile
from pathlib import Path

URL = ("https://www.bundesnetzagentur.de/DE/Fachthemen/Telekommunikation/Nummerierung/ONRufnr/_DL/"
       "Vorwahlverzeichnis_ONB.zip.zip")
TARGET = Path("shared/src/commonMain/kotlin/de/geosphere/congregationplaner/user/domain/model/GermanAreaCodes.kt")
MAX_LINE_CONTENT = 100
# Eine Textkonstante darf in der JVM höchstens 65535 Byte haben; die Daten werden deshalb aufgeteilt.
MAX_PART_BYTES = 50_000
FORBIDDEN = set('"$\\|=')


def main() -> None:
    with urllib.request.urlopen(URL, timeout=60) as response:
        archive = zipfile.ZipFile(io.BytesIO(response.read()))
    name = next(n for n in archive.namelist() if n.lower().endswith(".csv"))
    stand = re.search(r"(\d{4})(\d{2})(\d{2})", name)
    stand_text = f"{stand.group(3)}.{stand.group(2)}.{stand.group(1)}" if stand else "unbekannt"
    rows = csv.DictReader(io.StringIO(archive.read(name).decode("utf-8-sig")), delimiter=";")
    entries = {
        r["Ortsnetzkennzahl"]: r["Ortsnetzname"].strip()
        for r in rows
        if (r.get("Ortsnetzkennzahl") or "").isdigit() and (r.get("KennzeichenAktiv") or "").strip() == "1"
    }
    if len(entries) < 5000:
        raise SystemExit(f"Nur {len(entries)} Vorwahlen gelesen, erwartet über 5000; Datei nicht geschrieben.")
    bad = [n for n in entries.values() if FORBIDDEN & set(n)]
    if bad:
        raise SystemExit(f"Ortsnamen mit unerlaubten Zeichen: {bad[:5]}; Datei nicht geschrieben.")

    items = [f"{code}={entries[code]}|" for code in sorted(entries, key=lambda c: (len(c), c))]
    parts, current, size = [], [], 0
    for item in items:
        if size + len(item.encode()) > MAX_PART_BYTES:
            parts.append(current)
            current, size = [], 0
        current.append(item)
        size += len(item.encode())
    parts.append(current)

    def literal_lines(part):
        lines, line = [], ""
        for item in part:
            if len(line) + len(item) > MAX_LINE_CONTENT:
                lines.append(line)
                line = ""
            line += item
        lines.append(line)
        return " +\n".join(f'    "{line}"' for line in lines)

    constants = "\n\n".join(
        f"private const val AREA_PART_{i + 1} =\n{literal_lines(part)}" for i, part in enumerate(parts)
    )
    part_names = ", ".join(f"AREA_PART_{i + 1}" for i in range(len(parts)))
    TARGET.write_text(
        f"""// Erzeugt von scripts/update_german_area_codes.py – nicht von Hand ändern.
package de.geosphere.congregationplaner.user.domain.model

/**
 * Die aktiven deutschen Ortsnetzkennzahlen (Vorwahlen ohne führende 0) mit ihrem Ortsnetznamen, {len(entries)} Stück.
 *
 * Quelle: Bundesnetzagentur, Vorwahlverzeichnis (VwV), Stand {stand_text}. Lizenz: Datenlizenz Deutschland –
 * Namensnennung – Version 4.0. Bearbeitet: nur die aktiven Kennzahlen.
 */
internal val GERMAN_AREA_NAMES: Map<String, String> by lazy {{
    // Zur Laufzeit verbunden, damit der Compiler die Teile nicht zu einer zu großen Konstante zusammenfasst.
    listOf({part_names}).joinToString("")
        .split('|')
        .filter(String::isNotEmpty)
        .associate {{ it.substringBefore('=') to it.substringAfter('=') }}
}}

/** Die Ortsnetzkennzahlen aus [GERMAN_AREA_NAMES]. */
internal val GERMAN_AREA_CODES: Set<String> get() = GERMAN_AREA_NAMES.keys

/** Stand des Vorwahlverzeichnisses, für die Quellenangabe in der App. */
const val GERMAN_AREA_CODES_STAND = "{stand_text}"

{constants}
""",
        encoding="utf-8",
        newline="\n",
    )
    print(f"{len(entries)} Vorwahlen mit Ortsnamen (Stand {stand_text}) in {len(parts)} Teilen nach {TARGET} geschrieben.")


if __name__ == "__main__":
    main()
