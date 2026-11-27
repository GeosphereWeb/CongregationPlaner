# ADR-0041: Aushang der Vorträge als PDF mit eigenem Schreiber, Grafiken in Firestore

## Status
Accepted

## Context
Planer sollen aus den Vortragsterminen einen Aushang als PDF erzeugen, mit Titel, Grafik und Farbset für die ganze Versammlung. Er muss auf Android und Desktop gleich aussehen. Für die Grafiken und für die PDF-Erzeugung gab es je mehrere Wege.

## Decision
- Grafiken liegen in Firestore, je Grafik ein Dokument in `noticeImages` (PNG-Bytes, höchstens 700 000 Bytes und 2000 × 2000 Pixel, höchstens 10 je Versammlung). Sie werden einmal geladen und nie laufend abgefragt. Firebase Storage entfällt: für neue Speicher-Buckets ist laut Firebase ein kostenpflichtiger Tarif nötig (Stand 2026), dazu kommen ein zusätzliches SDK und auf dem Desktop ein eigener REST-Zugang für ein paar Logos.
- Titel, Farbset und Standardgrafik stehen im Dokument `settings/talkNotice`; das Farbset ist eines von fünf festen Sets. Nur Vortragsplaner schreiben, wer die Vortragsplanung sehen darf, liest.
- Das PDF erzeugt ein eigener, minimaler Schreiber in `jvmAndAndroidMain` (Standardschriften Helvetica und Helvetica-Bold, WinAnsi, daher „Nr.“ statt „№“). PDFBox, Android `PdfDocument` und HTML-Druck sind verworfen: zwei Layouts, neue Abhängigkeit bzw. zu wenig Kontrolle.
- Der Aushang hat immer genau eine A4-Seite mit fester Zeilenhöhe; die Zeilenzahl ist auf `MAX_NOTICE_ROWS = 11` begrenzt, Zeilen werden nie enger gesetzt.

## Consequences
- Kein neuer Dienst und keine Kosten; Bilder sind durch Dokumentgröße und Anzahl begrenzt, das Laden kostet einmal pro Öffnen so viele Lesezugriffe wie Grafiken; die Anzahl beim Hinzufügen wird serverseitig gezählt, ohne die Bilddaten zu laden.
- Ein Layout für beide Plattformen und testbar ohne Fremdbibliothek, aber nur PNG-Grafiken und nur Standardschriften ohne Einbettung; Zeichen außerhalb von WinAnsi werden ersetzt bzw. transliteriert.
- Mehr als 11 Einträge pro Aushang sind nicht möglich; ein zweiter Aushang ist nötig.
