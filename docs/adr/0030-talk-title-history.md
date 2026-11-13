# ADR-0030: Titelhistorie der Vortragsliste

## Status
Accepted

## Context
Die Titel der Vorträge in der globalen Vortragsliste (ADR-0003) ändern sich gelegentlich zu einem bestimmten Stichtag. Termine speichern Nummer und Titel als Momentaufnahme (ADR-0002); gehaltene Vorträge sollen ihren damaligen Titel behalten, geplante Termine ab dem Stichtag aber den neuen. Bisher gab es nur einen Titel; eine Änderung wirkte erst bei der nächsten Auswahl und nur, wenn der Planer neu auswählte.

Zur Wahl standen eine einzige geplante Änderung je Vortrag (alter Titel, neuer Titel, Stichtag) und eine volle Historie. Gewählt wurde die volle Historie, damit sich nachvollziehen lässt, wie ein Vortrag zu jedem Zeitpunkt hieß, und mehrere künftige Änderungen im Voraus eingetragen werden können.

## Decision
- Ein Dokument `talkCatalog/{Nummer}` hat neben `title` (dem ursprünglichen Titel) und `active` das Feld `titleChanges`: eine Liste von bis zu zehn Einträgen `{validFrom: 'yyyy-MM-dd', title}`. Die Rules verlangen alle drei Felder.
- `Talk.titleOn(date)` liefert den Titel, der an einem Tag gilt: die letzte Änderung bis zu diesem Tag, sonst den ursprünglichen Titel. `SaveTalkUseCase` sortiert die Änderungen nach Datum und erlaubt je Tag höchstens eine.
- Die Auswahl im Termin zeigt und übernimmt den Titel, der am Termintag gilt (`asOf`). Der Termin selbst bleibt eine Momentaufnahme.
- Weicht der gespeicherte Titel eines Termins vom gültigen ab, bietet der Termin-Editor „Titel aktualisieren“ an; ein inzwischen inaktiver Vortrag bekommt einen Warnhinweis. Es gibt kein automatisches Umschreiben der Termine.
- Der Import der Vortragsliste (ohne Historie in der Quelle) übernimmt die Titeländerungen des vorhandenen Vortrags.

## Consequences
- Gehaltene Termine ändern sich nie; geplante Termine werden erst geändert, wenn der Planer es im Editor bestätigt.
- Die Vortragsliste zeigt den heute gültigen Titel; die Pflege zeigt den ursprünglichen Titel und die Änderungen.
- Die Prüfung von zehn Änderungen bleibt in den Rules klein, weil das Dokument sonst nur zwei Felder hat.
- Termine mit veraltetem Titel fallen nur im Editor auf, nicht in der Liste.
