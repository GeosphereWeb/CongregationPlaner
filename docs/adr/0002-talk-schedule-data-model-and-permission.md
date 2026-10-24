# ADR-0002: Vortragsplanung – Datenmodell und Berechtigung

## Status
Accepted

## Context
Jede Versammlung plant ihre wöchentlichen Vorträge selbst. Ein Termin braucht einen eindeutigen Schlüssel, der Versammlungstag kann sich kurzfristig verschieben, und die Liste der Wochenereignisse (z. B. Kongress) ist für alle Versammlungen und Kreise gleich. Planen soll nur dürfen, wer dafür freigegeben ist; Bestandsadmins besitzen keine Einzelberechtigungen.

## Decision
- Termine liegen in `…/congregation/{id}/talkSchedule/{yyyy-MM-dd}`; das ISO-Datum ist die Dokument-ID, der Wochentag wird nicht geprüft. Das Verschieben eines Termins speichert den neuen Tag und löscht danach den alten (`MoveTalkSlotUseCase`).
- Wochenereignisse liegen global in `weekEvents/{id}` (`name`, `cancelsTalk`), werden außerhalb der App gepflegt und sind per Rules nur lesbar. Der Termin speichert ID und Namen des Ereignisses mit.
- Planen und Lesen erlauben globale Admins, freigegebene Versammlungsadmins und freigegebene Mitglieder mit der neuen Berechtigung `UserPermission.PLAN_TALKS` (`TalkSchedulePermissionPolicy`, gespiegelt in `canPlanTalks` der `firestore.rules`).

## Consequences
- Ein Termin pro Tag ist garantiert; ein Speichern auf einen belegten Tag überschreibt, daher prüft das ViewModel nur gegen das geladene Jahr. Beim Verschieben über den Jahreswechsel kann ein Termin im anderen Jahr überschrieben werden.
- Schlägt beim Verschieben das Löschen fehl, bleibt ein Duplikat statt eines Verlusts.
- Alte Termine bleiben lesbar, wenn sich ein Ereignisname ändert, werden aber nicht nachgezogen.
- `firestore.rules` und App-Version müssen zusammen ausgerollt werden, weil `PLAN_TALKS` bei den erlaubten Berechtigungen ergänzt wurde.
- Der Desktop-Client (REST) lädt die Termine ohne ID-Bereichsabfrage und filtert nach Jahr; das trägt bis ca. 1000 Termine.
