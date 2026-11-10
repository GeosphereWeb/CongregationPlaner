# ADR-0022: Globale Admins pflegen ihre Kontaktdaten im eigenen Admin-Dokument

## Status
Accepted

## Context
Globale Admins haben kein Mitgliedsdokument und sehen keine Versammlungsdaten (ADR-0013). „Mein Profil“ unter „Einstellungen“ speicherte aber im Mitgliedsdokument einer gemerkten Versammlung; ein globaler Admin bekam beim Speichern `PERMISSION_DENIED`, weil dort nichts existiert, das er ändern könnte. Es fehlte ein Ort für seine Kontaktdaten außerhalb jeder Versammlung. Das Dokument `admins/{uid}` existiert für jeden globalen Admin schon und wird nur von Hand angelegt.

## Decision
- Ein globaler Admin ohne Mitgliedsdokument (`AppUser.isMember == false`) speichert Nachnamen, Vornamen, Handy, Festnetz und E-Mail-Adressen in seinem Dokument `admins/{uid}`. Adresse und Notfallkontakte pflegt er wie bisher nicht.
- Die Rules erlauben dafür nur das **Ändern** des eigenen Admin-Dokuments mit genau diesen fünf Feldern (`isValidAdminProfile`). Anlegen und Löschen bleiben der Konsole vorbehalten, Auflisten bleibt verboten; das Dokument macht den Nutzer weiter nur durch sein Vorhandensein zum globalen Admin.
- Die App beobachtet das Admin-Dokument (`observeGlobalAdminProfile`) und legt die Daten in das Profil des zusammengestellten Nutzers. Ist der globale Admin zugleich Mitglied einer Versammlung, gilt wie bisher sein Mitgliedsdokument.

## Consequences
- Globale Admins können ihre Kontaktdaten pflegen, ohne einer Versammlung anzugehören; die Trennung von den Versammlungsdaten (ADR-0013) bleibt.
- Das Admin-Dokument trägt jetzt Daten einer Person. Es bleibt für andere Nutzer unlesbar (`get` nur für den Eigentümer).
- Ein Fehler beim Lesen der Kontaktdaten sperrt den globalen Admin nicht aus der App aus; sein Profil ist dann leer.
- Hat ein globaler Admin zusätzlich ein Mitgliedsdokument, sind die beiden Datensätze unabhängig voneinander.
