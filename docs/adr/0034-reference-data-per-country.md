# ADR-0034: Vortragsliste und Wochenereignisse je Land

## Status
Accepted

## Context
Vortragsliste und Wochenereignisse lagen global (`talkCatalog`, `weekEvents`, ADR-0002/ADR-0003) und galten damit für alle Versammlungen weltweit. Vortragsnummern, Titel und Ereignisse unterscheiden sich aber je Land, etwa nach Sprache. Der globale Admin hat keine eigene Versammlung (ADR-0013), aus der sich ein Land ergäbe.

## Decision
- Beide Listen liegen unter dem Land der Organisationsstruktur: `region/…/country/{id}/talkCatalog/{Nummer}` und `…/weekEvents/{id}`. Felder und Validierung bleiben unverändert.
- Mitglieder und Planer lesen die Listen des Landes ihrer Versammlung (`CongregationRef.country`).
- Der globale Admin wählt zum Pflegen und Importieren ein bestehendes Land (`ListCountriesUseCase`, `CountryChoiceHolder`); vorgewählt ist das Land der eigenen Versammlung oder das einzige vorhandene.
- Die Rules erlauben das Schreiben nur dem globalen Admin und nur unterhalb eines vorhandenen Landdokuments.

## Consequences
- Jedes Land braucht eigene Listen; ein neues Land startet leer und wird per Import oder von Hand befüllt.
- Die Länderauswahl liest die Organisationsstruktur Ebene für Ebene; bei vielen Regionen kostet das mehrere Abfragen.
- Ein Rules-Schreibzugriff kostet einen zusätzlichen `exists`-Aufruf auf das Land.
