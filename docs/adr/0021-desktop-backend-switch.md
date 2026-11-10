# ADR-0021: Desktop-App wählt Emulator oder Online-Datenbank über einen Schalter

## Status
Accepted

## Context
Die Desktop-App spricht Firestore über die REST-API an. Das war fest der lokale Emulator, die Anmeldung dagegen ging ohne Emulator-Variablen an die Online-Anmeldung. Der Import der Excel-Exporte (Wochenereignisse, Vortragsliste, Redner, Vortragsplanung) läuft nur auf dem Desktop und muss auch in die produktive Datenbank schreiben können. Unter Android entscheidet der Build-Typ (debuggable = Emulator, ADR-0019 und `MainActivity`); ein Desktop-Build hat keinen solchen Typ.

## Decision
- Ein Schalter `FIREBASE_USE_EMULATOR` (Umgebungsvariable, `.env` oder `firebase.useEmulator`) wählt für Anmeldung und Firestore gemeinsam: `true` die lokalen Emulatoren, `false` die Online-Dienste (`identitytoolkit.googleapis.com`, `firestore.googleapis.com`). Er hat Vorrang vor den einzelnen Emulator-Variablen (`DesktopEnv.useEmulatorSetting`).
- Ohne den Schalter bleibt alles wie bisher: Firestore am Emulator, die Anmeldung nach den vorhandenen Emulator-Variablen. Die Online-Datenbank wird nie versehentlich genutzt, nur auf ausdrückliches `false`.
- Online gelten die Firestore-Rules mit dem ID-Token des angemeldeten Nutzers; die Desktop-App bekommt keine Sonderrechte. Nötig sind `FIREBASE_PROJECT_ID` und `FIREBASE_WEB_API_KEY`.

## Consequences
- Der Import schreibt nur, was der angemeldete Nutzer laut Rules darf: Wochenereignisse und Vortragsliste als globaler Admin, Redner und Vortragsplanung als Vortragsplaner der Versammlung.
- Die Desktop-App beobachtet Daten durch Abfragen alle 15 Sekunden (`pollingFlow`). Online verursacht das Lesezugriffe, solange ein Bereich offen ist; für den Import genügt das, für den Dauerbetrieb wäre ein größerer Abstand oder ein Listener nötig.
- Ein Fehler im Schalter zeigt sich als Anmelde- oder Verbindungsfehler; die Oberfläche warnt nicht, dass die App gegen die Online-Datenbank arbeitet.
