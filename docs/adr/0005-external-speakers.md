# ADR-0005: Externe Redner je Versammlung ohne Verweis auf fremde Mitglieder

## Status
Accepted

## Context
Neben den eigenen Mitgliedern halten auch Redner aus anderen Versammlungen Vorträge. Mitglieder anderer Versammlungen zu referenzieren hieße, deren Mitgliedsdokumente lesbar zu machen, die bewusst nur Admins und Berechtigten der eigenen Versammlung offenstehen (ADR-0004).

## Decision
- Jede Versammlung pflegt eine eigene Liste `…/congregation/{id}/externalSpeakers/{id}` mit Familienname, Vorname und Versammlung des Redners als Freitext. Lesen, Anlegen, Ändern und Löschen dürfen nur Planer dieser Versammlung (`canPlanTalks`).
- Der Termin speichert einen externen Redner als Momentaufnahme (`externalSpeakerId`, die Redner-Namensfelder und `speakerCongregation`). Ein Termin hat höchstens ein Mitglied oder einen externen Redner als Redner; Use Case und Rules prüfen das.
- Externe Redner brauchen nicht die Berechtigung `VIEW_MEMBERS`, weil sie nicht aus den Mitgliedern stammen.

## Consequences
- Derselbe Redner kann in mehreren Versammlungen mehrfach erfasst sein; Tippfehler oder abweichende Schreibweisen der Versammlung werden nicht abgeglichen.
- Das Löschen eines externen Redners aus der Liste ändert bereits geplante Termine nicht, weil sie den Namen als Momentaufnahme tragen.
- Die Rules prüfen bei den Terminfeldern nur die Form, nicht ob die `externalSpeakerId` noch existiert.
