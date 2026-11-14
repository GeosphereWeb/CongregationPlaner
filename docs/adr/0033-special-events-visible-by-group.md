# ADR-0033: Besondere Ereignisse mit Sichtbarkeit je Gruppe

## Status
Accepted

## Context
Der Versammlungsadmin pflegt besondere Ereignisse (ein oder mehrere Tage), die auf der Startseite erscheinen. Nicht jedes Ereignis ist für alle bestimmt, z. B. eine Schulung nur für Älteste. Firestore-Regeln filtern keine Abfragen: Mitglieder dürfen nur Ereignisse abfragen, die sie sehen dürfen, sonst lehnt Firestore die ganze Abfrage ab.

## Decision
- Ereignisse liegen in `…/congregation/{id}/events/{id}` mit `title`, `startDate`, `endDate` (`yyyy-MM-dd`, eintägig gleich) und `audience`: `ALL` oder eine Auswahl aus `BROTHERS`, `SISTERS`, `ELDERS`, `MINISTERIAL_SERVANTS`.
- Die Gruppen eines Mitglieds werden aus Geschlecht, Taufe und Dienstamt abgeleitet (ADR-0031), gleich in `MemberProfile.eventGroups()` und `eventGroups()` in den Rules. Freigegebene Mitglieder lesen ein Ereignis, wenn `audience` eine ihrer Gruppen enthält; die App fragt dazu mit `array-contains-any` genau ihre Gruppen ab. Der Rules-Test belegt, dass Firestore diese Abfrage zulässt und fremde Gruppen ablehnt.
- Pflegen (lesen, anlegen, ändern, löschen) darf nur der Versammlungsadmin. „Alle“ schließt die übrigen Gruppen aus.

## Consequences
- Keine eigenen Gruppen-Dokumente oder Mitgliederlisten pro Ereignis; neue Gruppen brauchen eine Änderung an App und Rules.
- Jede Leseprüfung kostet in den Rules einen Zugriff auf das eigene Mitgliedsdokument.
- Ändert sich das Dienstamt oder die Taufe eines Mitglieds, ändert sich sofort, welche Ereignisse es sieht.
