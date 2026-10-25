# ADR-0007: Vortragshistorie je Versammlung als eigene Subcollection

## Status
Accepted

## Context
Die Planung soll später kürzlich gehaltene Vorträge nicht vorschlagen und erkennen, welche Vorträge noch nie gehalten wurden. Die Termine in `talkSchedule` enthalten zwar Vortragsnummern, doch früher gehaltene Vorträge (vor der Nutzung der App) lassen sich dort nicht nachtragen, und die Auswertung müsste alle Jahre lesen.

## Decision
- Die Historie liegt in `…/congregation/{id}/talkHistory/{Vortragsnummer}` mit `heldOn` (Liste von ISO-Daten, 1 bis 100 Einträge) und `plannedOn` (die Tage aus `heldOn`, die aus der Planung stammen). Ein Vortrag ohne Dokument gilt als noch nie gehalten.
- Lesen, Schreiben und Löschen dürfen nur Planer der Versammlung (`canPlanTalks`), wie bei `talkSchedule`; `TalkHistoryRepository` und die Use Cases sortieren und bereinigen die Tage.
- Eine einfache Übersicht (`TalkHistoryContent`, Liste/Detail nach ADR-0006) zeigt je Vortrag der Vortragsliste den letzten Tag oder „noch nie gehalten“ und im Detail alle Tage, ohne etwas zu ändern. Vorschlagslogik folgt separat.
- Beim Speichern, Verschieben oder Löschen eines Termins trägt `SyncTalkHistoryWithSlotUseCase` den Tag beim Vortrag des Termins als geplanten Tag ein und entfernt ihn beim bisherigen Vortrag; ein abgesagter Vortrag zählt nicht. Die Historie ändert sich damit nur durch die Planung; Nachtragen und Löschen sind in der Übersicht nicht möglich, Repository und Use Cases (`SaveTalkHistoryEntryUseCase`, `DeleteTalkHistoryEntryUseCase`) bleiben dafür vorhanden.

## Consequences
- Ein geplanter Termin in der Zukunft zählt schon als gehalten. Schlägt nur die Historie fehl, bleibt der Termin gespeichert, und die Meldung weist darauf hin; Termin und Historie können dann auseinanderlaufen.
- Ein Dokument je Vortrag hält die Abfrage klein (höchstens 9999 Dokumente), jede Änderung überschreibt aber die ganze Tageliste.
- `firestore.rules` muss zusammen mit der App ausgerollt werden, sonst lehnt Firestore Zugriffe auf `talkHistory` ab.
