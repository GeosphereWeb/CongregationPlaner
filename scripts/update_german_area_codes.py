"""Erzeugt GermanAreaCodes.kt aus dem Vorwahlverzeichnis der Bundesnetzagentur.

Aufruf aus dem Repo-Root:  python scripts/update_german_area_codes.py

Quelle: Bundesnetzagentur, Vorwahlverzeichnis (VwV), ZIP mit einer CSV-Datei
(https://www.bundesnetzagentur.de/DE/Fachthemen/Telekommunikation/Nummerierung/ONRufnr/Einteilung_ONB/start.html).
Lizenz: Datenlizenz Deutschland – Namensnennung – Version 4.0 (laut Impressum der Bundesnetzagentur für .csv-Dateien,
falls nicht anders gekennzeichnet). Übernommen werden nur die aktiven Ortsnetzkennzahlen; die Ortsnamen nicht.
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
CODES_PER_LINE = 18


def main() -> None:
    with urllib.request.urlopen(URL, timeout=60) as response:
        archive = zipfile.ZipFile(io.BytesIO(response.read()))
    name = next(n for n in archive.namelist() if n.lower().endswith(".csv"))
    stand = re.search(r"(\d{4})(\d{2})(\d{2})", name)
    stand_text = f"{stand.group(3)}.{stand.group(2)}.{stand.group(1)}" if stand else "unbekannt"
    text = archive.read(name).decode("utf-8-sig")
    rows = csv.DictReader(io.StringIO(text), delimiter=";")
    codes = sorted(
        {r["Ortsnetzkennzahl"] for r in rows
         if (r.get("Ortsnetzkennzahl") or "").isdigit() and (r.get("KennzeichenAktiv") or "").strip() == "1"},
        key=lambda c: (len(c), c),
    )
    if len(codes) < 5000:
        raise SystemExit(f"Nur {len(codes)} Vorwahlen gelesen, erwartet über 5000; Datei nicht geschrieben.")

    chunks = [",".join(codes[i:i + CODES_PER_LINE]) for i in range(0, len(codes), CODES_PER_LINE)]
    body = " +\n".join(f'    "{chunk},"' for chunk in chunks)
    TARGET.write_text(
        f"""// Erzeugt von scripts/update_german_area_codes.py – nicht von Hand ändern.
package de.geosphere.congregationplaner.user.domain.model

/**
 * Die aktiven deutschen Ortsnetzkennzahlen (Vorwahlen ohne führende 0), {len(codes)} Stück.
 *
 * Quelle: Bundesnetzagentur, Vorwahlverzeichnis (VwV), Stand {stand_text}. Lizenz: Datenlizenz Deutschland –
 * Namensnennung – Version 4.0. Bearbeitet: nur die aktiven Kennzahlen, ohne Ortsnamen.
 */
internal val GERMAN_AREA_CODES: Set<String> by lazy {{ AREA_CODES.split(',').filter(String::isNotEmpty).toHashSet() }}

/** Stand des Vorwahlverzeichnisses, für die Quellenangabe in der App. */
const val GERMAN_AREA_CODES_STAND = "{stand_text}"

private const val AREA_CODES =
{body}
""",
        encoding="utf-8",
        newline="\n",
    )
    print(f"{len(codes)} Vorwahlen (Stand {stand_text}) nach {TARGET} geschrieben.")


if __name__ == "__main__":
    main()
