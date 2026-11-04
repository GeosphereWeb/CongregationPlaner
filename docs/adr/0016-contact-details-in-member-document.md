# ADR-0016: Adresse und Notfallkontakte im Mitgliedsdokument

## Status
Accepted (ersetzt ADR-0015)

## Context
ADR-0015 hat Adresse und Notfallkontakte in ein eigenes Dokument `memberPrivate/{uid}` gelegt, damit Planer sie nicht sehen. Das bedeutet zwei Collections, zwei Lesewege und dass Admins die Angaben nur lesen, aber nicht pflegen können. Die Versammlung soll Admins und Planern alle Mitgliederdaten zeigen; die Admins sollen alles pflegen können, auch bei Personen ohne Konto.

## Decision
- Adresse (Straße, Postleitzahl, Ort) und bis zu fünf Notfallkontakte stehen als Felder im Mitgliedsdokument `…/members/{uid}`; `memberPrivate` entfällt.
- Das Mitglied ändert im eigenen Dokument nur seine Kontaktangaben (Namen, Telefon, E-Mail-Adressen, Adresse, Notfallkontakte; `ownFields()` in den Rules). Kategorie, Taufe, Dienstamt, Redner, Freigabe und Rollen ändert nur der Versammlungsadmin, der bei anderen Mitgliedern alle Felder pflegt.
- Lesen dürfen wie bisher das Mitglied selbst, die Versammlungsadmins und die Planer (ADR-0012, ADR-0013).

## Consequences
- Planer sehen Adresse und Notfallkontakte, auch über die Firebase-SDKs.
- Rules werten höchstens 1000 Ausdrücke pro Anfrage aus. Mit der übrigen Prüfung des Mitgliedsdokuments passt eine Prüfung jeder Notfallnummer nicht mehr hinein; die Rules prüfen bei Notfallkontakten nur Felder, Namen und die Zahl der Nummern, den Inhalt der Nummern prüft die App (wie bei `talkNumbers`).
- Bestehende `memberPrivate`-Dokumente werden nicht übernommen.
