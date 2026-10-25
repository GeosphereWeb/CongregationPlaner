# ADR-0008: Verzeichnis externer Redner mit eigener Hierarchie Kreis → Versammlung → Redner

## Status
Accepted

## Context
Die flache Liste aus ADR-0005 hält die Versammlung eines externen Redners nur als Freitext. Der Planer kann Redner daher weder nach Kreis oder Versammlung ordnen noch Telefon, E-Mail oder die Vorträge eines Redners erfassen. Kreise und Versammlungen der Organisationsstruktur (`region/…/circuit/…/congregation`) sind dafür ungeeignet: Dort legen globale Admins die Einträge an, und die externen Versammlungen sind meist nicht in der App erfasst.

## Decision
- Unter der eigenen Versammlung pflegt der Planer ein eigenes Verzeichnis in drei flachen Subcollections: `externalCircuits` (Name), `externalCongregations` (Name, `circuitId`) und `visitingSpeakers` (Familienname, Vorname, Telefon, E-Mail, `congregationId`, `talkNumbers`). Es hat keinen Bezug zur Organisationsstruktur. Lesen und Schreiben dürfen nur Planer der Versammlung (`canPlanTalks`).
- Die Subcollections sind flach mit Fremdschlüssel statt verschachtelt, damit Picker und Verwaltung das ganze Verzeichnis mit drei Abfragen laden, ohne Collection-Group-Abfragen. Die Use Cases (`ExternalSpeakerDirectoryUseCases`) prüfen, dass der Verweis existiert, und verweigern das Löschen eines Kreises oder einer Versammlung mit Untereinträgen.
- Die alte Liste `externalSpeakers` entfällt, ihre Rules ebenfalls. Der Termin speichert den Redner weiter als Momentaufnahme (Name und Versammlungsname); `ExternalSpeaker` bleibt das Auswahl-Element des Pickers und wird aus dem Verzeichnis abgeleitet. Angelegt und gelöscht werden Redner nur noch in der neuen Verwaltung (`ExternalDirectoryContent`, Liste/Detail nach ADR-0006), nicht mehr im Picker.

## Consequences
- Bereits in der alten Liste erfasste Redner werden nicht übernommen; geplante Termine bleiben lesbar, weil sie ihren Namen als Momentaufnahme tragen.
- Die Rules prüfen nur die Form, weder ob `circuitId` und `congregationId` existieren noch die Elemente von `talkNumbers` (Zahlen 1 bis 9999); das übernimmt die App.
- Beim Löschen prüft die App nur den Stand, den sie gerade kennt. Legt ein anderer Planer gleichzeitig einen Untereintrag an, kann eine Versammlung ohne Kreis zurückbleiben; sie erscheint dann nicht in der Verwaltung.
- `firestore.rules` muss zusammen mit der App ausgerollt werden, sonst lehnt Firestore Zugriffe auf die neuen Subcollections ab.
