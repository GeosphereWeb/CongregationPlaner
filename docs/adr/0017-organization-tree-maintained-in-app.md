# ADR-0017: Organisationsbaum in der App pflegen

## Status
Accepted

## Context
Regionen, Subregionen und Zweige ließen sich nur in der Firebase-Konsole anlegen. Kreise und Versammlungen pflegte die App unter festen Pfaden (`region/EMEA/subregion/europe/branch/central-europe` bzw. Kreis `609a`); Versammlungen anderer Kreise waren nicht erreichbar. Die Rules erlaubten dem globalen Admin auf diesen Ebenen beliebige Felder.

## Decision
- Ein Bereich „Organisation“ in der Globalen Verwaltung ersetzt „Kreise“ und „Versammlungen“: Regionen → Subregionen → Zweige → Kreise → Versammlungen zum Durchklicken, mit Pfadzeile und System-Zurück. Ein geöffneter Kreis zeigt die vorhandene Versammlungsverwaltung für genau diesen Kreis.
- Region bis Kreis sind `OrgUnit`s (Name, beim Kreis zusätzlich der Kreisaufseher) mit einem gemeinsamen `OrgUnitRepository`; die Versammlung behält `CongregationMetadataRepository`, das den Kreis als Parameter bekommt.
- IDs erzeugt Firestore beim Anlegen; sie sind danach fest, weil sie Pfadsegmente sind. Angezeigt wird überall der Name. Verschieben unter eine andere Einheit gibt es nicht.
- Region bis Kreis lassen sich nur löschen, wenn die nächste Ebene leer ist; die App prüft das vorher, weil Firestore Unterdokumente nicht mitlöscht und die Rules es nicht prüfen können.
- Die Rules erlauben auf diesen Ebenen nur `name` bzw. beim Kreis `name` und `overseer_name`.

## Consequences
- Bestehende Einheiten mit lesbaren IDs (`EMEA`, `609a`) bleiben; neue haben zufällige IDs, die in Einladungslinks verschlüsselt stehen.
- Eine Versammlung kann der globale Admin weiterhin löschen, ohne dass geprüft wird, ob sie leer ist: Er darf ihre Mitglieder nicht lesen (ADR-0013). Das Anlegen einer Versammlung samt erstem Admin bleibt wie in ADR-0013 und braucht später eine bessere Lösung.
- Das Löschen leerer Einheiten verlässt sich auf die App; über die Firebase-SDKs kann ein globaler Admin auch nicht leere Einheiten löschen und Unterdokumente verwaisen lassen.
