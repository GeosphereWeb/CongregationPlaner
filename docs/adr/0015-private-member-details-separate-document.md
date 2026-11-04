# ADR-0015: Adresse und Notfallkontakte in einem eigenen Dokument

## Status
Superseded by ADR-0016

## Context
Mitglieder sollen ihre Adresse und Notfallkontakte selbst pflegen. Das Mitgliedsdokument lesen aber neben den Versammlungsadmins auch alle Vortrags- und Aufgabenplaner (ADR-0012), und Firestore-Rules schränken nur ganze Dokumente ein, nicht einzelne Felder. Handy und Festnetz stehen schon im Mitgliedsdokument und sollen für die Planung sichtbar bleiben.

## Decision
- Adresse (Straße und Hausnummer, Postleitzahl, Ort) und bis zu fünf Notfallkontakte (Name, Beziehung, Telefon) stehen unter `…/congregation/{id}/memberPrivate/{uid}`, getrennt vom Mitgliedsdokument.
- Lesen dürfen nur das Mitglied selbst und die Versammlungsadmins; schreiben darf nur das Mitglied, und nur wenn es in dieser Versammlung ein Mitgliedsdokument hat. Löschen dürfen zusätzlich die Admins, damit beim Entfernen eines Mitglieds nichts zurückbleibt.
- Handy und Festnetz bleiben im Mitgliedsdokument; das Mitglied darf sie dort neben seinen Namen selbst ändern, Admins weiterhin auch.

## Consequences
- Planer sehen Adresse und Notfallkontakte nicht, auch nicht über die Firebase-SDKs.
- Die App liest für ein Mitglied zwei Dokumente; die privaten Angaben fehlen, solange das Mitglied sie nie gespeichert hat.
- Rules kennen keine Schleifen; die Notfallkontakte werden deshalb einzeln bis zur Obergrenze von fünf geprüft. Eine höhere Grenze braucht mehr Prüfzeilen.
- Admins können die privaten Angaben nur lesen, nicht ändern; für Personen ohne Konto gibt es damit keine Adresse oder Notfallkontakte.
