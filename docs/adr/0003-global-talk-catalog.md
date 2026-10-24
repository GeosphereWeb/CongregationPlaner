# ADR-0003: Globale Vortragsliste mit Titel-Momentaufnahme im Termin

## Status
Accepted

## Context
Alle Versammlungen und Kreise nutzen dieselbe Vortragsliste. Die Nummer eines Vortrags bleibt stabil, der Titel ändert sich gelegentlich, und ein Vortrag kann zeitweise nicht aktiv sein. Bereits geplante Termine dürfen sich durch eine Titeländerung nicht verändern.

## Decision
- Die Liste liegt global in `talkCatalog/{Nummer}` (`title`, `active`); die Dokument-ID ist die Vortragsnummer. Sie wird wie die Wochenereignisse außerhalb der App gepflegt und ist per Rules nur lesbar.
- Ein Termin speichert `talkNumber` und `talkTitle` als Momentaufnahme; beide gehören zusammen und sind in Use Case und Rules gemeinsam validiert. Die Auswahl bietet nur aktive Vorträge an, ein inzwischen deaktivierter Vortrag bleibt am Termin sichtbar.

## Consequences
- Titeländerungen in der Liste wirken nur auf neu geplante Termine; bestehende werden nicht nachgezogen.
- Lesen darf jeder angemeldete Nutzer, weil die Rules bei einer globalen Collection keine Versammlung prüfen können; die Liste enthält keine personenbezogenen Daten.
- Solange es keine Pflege-Oberfläche gibt, legt ein globaler Admin Vorträge manuell in Firestore an. Ein Dokument ohne Zahl als ID wird übersprungen.
