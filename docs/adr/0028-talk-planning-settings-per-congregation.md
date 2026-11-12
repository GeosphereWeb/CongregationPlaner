# ADR-0028: Stichtage des Planungsassistenten pro Versammlung

## Status
Accepted

## Context
Der Planungsassistent (`docs/vortragsplanung-assistent-plan.md`) schlägt nur Redner und Vorträge vor, die seit einem Stichtag nicht eingeplant waren. Die Stichtage sollen für alle Planer einer Versammlung gleich sein, nicht pro Gerät. Das Versammlungsdokument selbst ist öffentlich lesbar und nur vom Versammlungsadmin änderbar.

## Decision
- Die Stichtage liegen im Dokument `…/congregation/{id}/settings/talkPlanning` mit `speakerFreeSince` und `talkFreeSince` (je `yyyy-MM-dd` oder `null` = keine Sperre).
- Lesen dürfen Planer und Vortragshelfer, schreiben nur Planer; die Regeln lassen in `settings` nur dieses Dokument mit genau diesen Feldern zu, gelöscht wird es nie.
- Die App beobachtet die Collection `settings` mit den vorhandenen Collection-Helfern und nimmt das Dokument `talkPlanning`; ohne Dokument gelten keine Stichtage. Planer ändern sie oben in der Vortragsplanung, die Änderung wird sofort gespeichert.

## Consequences
- Alle Planer sehen dieselben Stichtage; weitere Einstellungen der Versammlung können als eigene Dokumente in `settings` dazukommen (Regeln je Dokument erweitern).
- Ein ungültiges gespeichertes Datum gilt als nicht gesetzt, statt das Laden zu blockieren.
