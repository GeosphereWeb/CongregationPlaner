# Firestore-Rules-Test

Prüft `../firestore.rules` gegen den Firestore- und den Auth-Emulator mit echten, angemeldeten Test-Nutzern. Der Test findet Kompilierfehler der Rules (z. B. mehr als 10 `let`-Variablen pro Funktion, siehe CP-31) und prüft, dass die Rules erlauben, was sie erlauben sollen, und alles andere ablehnen.

## Voraussetzungen

- Node.js ab Version 18 (der Test nutzt das eingebaute `fetch`)
- Java (für den Firestore-Emulator)
- Firebase CLI: `npm install -g firebase-tools` (lädt beim ersten Lauf die Emulatoren)

## Aufruf

Aus dem Repo-Root, unter Windows in PowerShell oder Git Bash:

```
node firebase-rules-test/run.mjs
```

`run.mjs` kopiert `firestore.rules` in diesen Ordner (die Firebase CLI erlaubt keine Rules außerhalb des Projektordners; die Kopie ist per `.gitignore` ausgeschlossen), startet die Emulatoren und führt `rules_test.mjs` aus. Der Exit-Code ist `0`, wenn alle Prüfungen bestehen, sonst `1`; jede fehlgeschlagene Prüfung steht mit Namen und der Antwort des Emulators in der Ausgabe.

Der Test läuft auf eigenen Ports (Firestore 18080, Auth 19099, Hub 14400, Logging 14500, WebSocket 19150). Ein laufender Entwicklungs-Emulator (8080, 9099) stört nicht und bekommt keine Testdaten.

## Was geprüft wird

- **Termine** (`talkSchedule`): minimal, mit Mitglied als Redner und Vorsitzendem, externer Redner, Redner = Vorsitzender (abgelehnt), Mitglied und externer Redner zugleich (abgelehnt), unvollständige Redner- und Vorsitzenden-Felder, Vortrag mit und ohne Titel, Wochenereignis, ungültige Datums-IDs, unbekannte Felder, ungültige Uhrzeit, Lesen und Löschen durch Planer und Nutzer ohne `PLAN_TALKS`.
- **Verzeichnis externer Redner** (`externalCircuits`, `externalCongregations`, `visitingSpeakers`): Anlegen und Lesen durch Planer, Formfehler (leere Namen, fehlende Verweise, Vortragsliste, Telefon zu lang, Zusatzfeld), Nutzer ohne `PLAN_TALKS`, die alte Liste `externalSpeakers` ist nicht mehr beschreibbar.
- **Vortragshistorie** (`talkHistory`): Anlegen, Lesen und Löschen durch Planer, ungültige Nummer, leere Tage, geplante Tage (`plannedOn`), Redner (`speakers`), Zusatzfeld, Nutzer ohne `PLAN_TALKS`.
- **Mitglieder lesen** (`members`): Planer mit und ohne `VIEW_MEMBERS`, das eigene Mitglied, `VIEW_MEMBERS` darf nicht schreiben, globale Admins.
- **Admin-Kontakte** (`adminContacts`): Vor- und Nachname der Admins sind für jedes Mitglied der Versammlung lesbar, auch ohne Freigabe; Mitglieder-Dokumente bleiben gesperrt. Schreiben nur durch Admins, nur für freigegebene Admins, mit passendem Namen und ohne Zusatzfelder; fremde Versammlungen und Nutzer ohne Mitgliedsdokument sind ausgeschlossen.
- **Vortragsliste und Wochenereignisse** (`talkCatalog`, `weekEvents`): Lesen für angemeldete Nutzer, Schreiben und Löschen nur für globale Admins, Formvalidierung (Nummer 1 bis 9999 ohne führende Null, Titel und Namen).

Nicht abgedeckt sind bisher die Organisationsstruktur, die Selbstregistrierung und das Verwalten der Mitglieder durch Admins.

## Neue Fälle ergänzen

In `rules_test.mjs` genügt eine Zeile pro Fall:

```js
await expect('Beschreibung des Falls', api('PATCH', 'pfad/dokument', fields({ feld: 'wert' }), planner.token), true);
```

`true` erwartet, dass die Rules den Zugriff erlauben (HTTP 200), `false`, dass sie ihn ablehnen (HTTP 403). Die Test-Nutzer (`admin`, `planner`, `viewer`, `plain`) und ihre Mitgliedsdokumente legt das Skript am Anfang mit dem Owner-Token an, der die Rules umgeht.

## Hinweis

Firestore erlaubt höchstens 10 `let`-Variablen pro Funktion in den Rules. Größere Validierungen deshalb in kleine Hilfsfunktionen aufteilen.

Der Test läuft bisher nicht in der CI (dafür bräuchte der Workflow Java, Node und die Firebase CLI); vor dem Commit einer Änderung an `firestore.rules` ist er von Hand auszuführen.
