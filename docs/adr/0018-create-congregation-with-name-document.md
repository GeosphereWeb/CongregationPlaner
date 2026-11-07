# ADR-0018: Versammlung selbst anlegen, Name pro Kreis über ein Namensdokument

## Status
Accepted

## Context
Nur globale Admins konnten Versammlungen anlegen, und ein neuer Nutzer konnte sich nie selbst zum Admin machen: Die Selbstregistrierung verlangt `approved == false` und keine Rollen (ADR-0012). Wer eine neue Versammlung braucht, soll sie selbst anlegen und ihr erster Admin werden. Firestore kennt keine Unique-Constraints, und die IDs der Versammlungen sind zufällig (ADR-0017); der Name muss im Kreis trotzdem eindeutig sein.

## Decision
- Ein angemeldeter Nutzer mit **bestätigter E-Mail-Adresse** (`email_verified` im Token) legt im gewählten Kreis eine Versammlung an. Die Auswahl im Login merkt Kreis und Namen vor; angelegt wird nach der Anmeldung. Globale Admins legen im Bereich „Organisation“ über denselben `CreateCongregationUseCase` an, ohne Mitglied zu werden (ADR-0013).
- Die Eindeutigkeit erzwingt ein **Namensdokument** `…/circuit/{id}/uniqueCongregationNames/{key}` mit `congregationId`; `key` ist der Name getrimmt, klein geschrieben und mit zusammengezogenen Leerzeichen. Versammlung, Namensdokument und (beim Nutzer) das Admin-Mitglied entstehen in **einem Batch**. Die Rules prüfen mit `existsAfter`/`getAfter`, dass alle zusammengehören; ein vergebener Name scheitert atomar, auch bei gleichzeitigem Anlegen.
- Die Versammlung trägt `createdBy`. Das Mitgliedsdokument des Erstellers ist nur im Schreibzugriff erlaubt, der die Versammlung anlegt (`!exists` vorher, `existsAfter` nachher, `createdBy == uid`): freigegeben, genau die Rolle `CONGREGATION_ADMIN`. Niemand kann sich so zum Admin einer bestehenden Versammlung machen.
- Umbenennen und Löschen führen das Namensdokument im selben Batch mit; die Rules geben einen Namen nur frei, wenn die Versammlung danach weg ist oder einen anderen Namen trägt.
- Registrierung fragt Vor- und Nachname. Firebase Auth speichert „Vorname Nachname“ als `displayName`; beim ersten Anlegen des Mitgliedsdokuments wird er am letzten Leerzeichen getrennt (im Profil korrigierbar).

## Consequences
- Jede Versammlung braucht ihr Namensdokument; Löschen und Umbenennen laufen über die App (Batch). Wer die SDKs umgeht, kann Namensdokumente verwaisen lassen.
- Auf dem Desktop trägt erst ein neuer Token (neue Anmeldung) die bestätigte E-Mail-Adresse; auf Android holt „Ich habe bestätigt“ den Token neu.
- Der Name „Vorname Nachname“ kann bei Doppelnamen falsch getrennt werden.
- Jeder bestätigte Nutzer kann Versammlungen anlegen; Missbrauch begrenzt vorerst nur die E-Mail-Bestätigung. Aufräumen leerer Versammlungen bleibt Sache der globalen Admins.
