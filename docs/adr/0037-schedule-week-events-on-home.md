# ADR-0037: Termine mit Wochenereignis auf der Startseite

## Status
Accepted

## Context
Unter „Besondere Ereignisse“ auf der Startseite standen nur die Ereignisse, die der Versammlungsadmin anlegt. Termine der Wochenendplanung mit Wochenereignis (z. B. Kreiskongress, Besuch des Kreisaufsehers) betreffen aber alle Mitglieder. Die Termine lesen bisher nur Vortragsplaner und Helfer. Wie weit die Startseite vorausschaut, soll der Versammlungsadmin festlegen.

## Decision
- Freigegebene Mitglieder dürfen die Termine lesen, in denen `weekEventId` gesetzt ist (`resource.data.get('weekEventId', null) != null`). Die App fragt sie gezielt mit diesem Filter ab (`observeWeekEventSlots`, Firestore `IS_NOT_NULL`), denn nur so kann die Regel die Abfrage freigeben. Alle anderen Termine bleiben Planern und Helfern vorbehalten.
- Den Zeitraum speichert die Versammlung im Feld `event_horizon` ihres Dokuments: `<1..365>D` oder `<1..24>M`, Standard drei Monate. Das Dokument ist ohnehin lesbar, und der Versammlungsadmin pflegt es schon (Zusammenkunftszeiten). Für eine eigene Einstellungs-Collection wären neue Regeln und Repositories nötig gewesen.
- Die Startseite führt beide Quellen zu `HomeEvent` zusammen. Titel und Icon eines Termins stammen aus seiner Ereignisart (ADR-0036), ersatzweise aus dem mitgespeicherten Namen. Die Ereignisse des Admins zeigt sie wie bisher ohne Zeitraum.

## Consequences
- Mitglieder sehen in Terminen mit Wochenereignis alle Felder, also auch Redner, Vorsitz und Notiz. Firestore kann einzelne Felder nicht sperren. Das ist vertretbar, weil diese Wochen ohnehin in der Versammlung bekannt gegeben werden.
- Fehlt die Regel oder schlägt die Abfrage fehl, zeigt die Startseite trotzdem die Ereignisse des Admins.
- Versammlungsdokumente ohne `event_horizon` erfüllen die Regeln erst, wenn sie neu gespeichert werden. Altdaten werden nicht migriert (Testumgebung).
