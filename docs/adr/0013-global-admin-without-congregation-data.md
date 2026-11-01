# ADR-0013: Der globale Admin hat keinen Einblick in Versammlungsdaten

## Status
Accepted

## Context
Globale Admins (`admins/{uid}`) verwalten Kreise, Versammlungen, die Vortragsliste und die Wochenereignisse. Bisher durften sie zusätzlich in jeder Versammlung Mitglieder samt Kontaktdaten lesen und ändern, Vorträge planen und die Versammlungsdaten bearbeiten, und sie wählten beim Login eine Versammlung. Seit ADR-0012 steuern Rollen, wer in einer Versammlung was darf; der globale Admin gehört zu keiner Versammlung und soll deren Mitgliederdaten nicht sehen.

## Decision
- Die Prüfungen für Inhalte einer Versammlung (Mitglieder lesen, ändern und löschen, Admin-Kontakte schreiben, Vortragsplanung samt Verzeichnis und Historie) kennen den globalen Admin nicht mehr, weder in den Policies noch in `firestore.rules`. Kreise, Wochenereignisse, die Vortragsliste sowie das Anlegen und Löschen von Versammlungen bleiben beim globalen Admin.
- Den Namen und die Stammdaten einer Versammlung darf der globale Admin weiter ändern; die Zusammenkunftszeiten ändert nur der Versammlungsadmin der eigenen Versammlung.
- Den ersten Admin einer neuen Versammlung trägt jemand einmalig in der Firebase-Konsole ein (`roles` und `approved` im Mitgliedsdokument); die App bietet dafür keinen Weg.
- Ein globaler Admin meldet sich ohne Versammlung an, sieht nur die Globale Verwaltung und bekommt kein Mitgliedsdokument. Ist er zugleich Mitglied einer Versammlung, gelten dort wie für alle nur seine Rollen.

## Consequences
- `firestore.rules` und App müssen zusammen ausgerollt werden; Konten, die ein globaler Admin früher beim Login in Versammlungen angelegt hat, bleiben als nicht freigegebene Mitglieder ohne Rolle bestehen und können entfernt werden.
- Eine neue Versammlung ist erst nach dem Konsoleneintrag des ersten Admins nutzbar; ein Admin, der alle Admin-Rollen einer Versammlung verliert, kann nur über die Konsole ersetzt werden.
- Die Navigation folgt ohne eigene Regel, weil sie aus den Policies abgeleitet wird.
- Ersetzt die Rechte globaler Admins auf Versammlungsdaten aus ADR-0002, ADR-0004, ADR-0009 und ADR-0011.
