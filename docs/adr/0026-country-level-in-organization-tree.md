# ADR-0026: Land als eigene Ebene zwischen Zweig und Kreis

## Status
Accepted

## Context
Ein Zweig (z. B. Zentraleuropa) umfasst mehrere Länder; die Kreise hingen bisher direkt am Zweig. Offen war, ob ein Land als ISO-Code (`DE`) oder als Name (`Deutschland`) gespeichert wird.

## Decision
- Neue Ebene `OrgLevel.COUNTRY` (Collection `country`): `region/…/subregion/…/branch/…/country/{countryId}/circuit/…/congregation/…`. `CongregationRef` hat dafür `countryId`.
- Ein Land ist eine normale Organisationseinheit wie Region, Subregion und Zweig: Firestore-Auto-ID als Dokument-ID, nur das Feld `name` mit dem Anzeigenamen (z. B. „Deutschland“). Kein ISO-Code, solange die App ihn nicht maschinell auswertet.
- Die Rules-Funktion `mirrorsAdminMember` bekommt den Mitgliedspfad statt der Einzel-IDs, weil Firestore-Rules-Funktionen höchstens 7 Argumente erlauben.

## Consequences
- Auswahl im Login, Organisationspflege und Einladungslinks laufen über die generische Ebenenliste und brauchen keinen Sonderfall.
- Bestehende Daten unter `branch/{id}/circuit` sind nicht mehr erreichbar und werden in der Testumgebung neu angelegt (keine Migration).
- Braucht die App später einen Ländercode (Telefonvorwahl, Feiertage, Flaggen), kommt er als zusätzliches Feld `code` (ISO 3166-1 alpha-2) dazu.
