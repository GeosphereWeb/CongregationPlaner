# ADR-0011: Admin-Namen sind vor der Freigabe lesbar

## Status
Accepted
Teilweise ersetzt durch [ADR-0012](0012-congregation-roles-list.md)

## Context
Wer sich neu registriert, wartet auf die Freigabe durch einen Versammlungsadmin und muss wissen, an wen er sich wenden kann. Nutzer ohne Freigabe dürfen keine Mitglieder lesen, und Firestore-Rules schränken nur ganze Dokumente ein, nicht einzelne Felder. Cloud Functions gibt es im Projekt nicht.

## Decision
- Unter `…/congregation/{id}/adminContacts/{uid}` steht je freigegebenem Admin ein Dokument mit nur `firstName` und `lastName`. Jeder angemeldete Nutzer mit einem Mitgliedsdokument in dieser Versammlung darf es lesen, auch ohne Freigabe; Mitglieder bleiben für ihn gesperrt.
- Admins schreiben ein Kontakt-Dokument nur, wenn das Mitgliedsdokument nach dem Schreiben (`getAfter`) ein freigegebener Admin mit demselben Namen ist. Jeder darf seinen eigenen Kontakt löschen.
- Die App pflegt die Kontakte (`AdminContactSyncingUserRepository`): Ein Admin wird zuerst gespeichert und dann veröffentlicht, ein entfernter oder degradierter Admin verliert zuerst den Kontakt. Admins veröffentlichen ihren Kontakt außerdem bei jeder Anmeldung, das füllt Bestandsdaten und berichtigt Abweichungen.
- Hat ein Admin noch keinen Namen, zeigt die App „Name nicht hinterlegt“; ohne Admin steht dort „kein Admin vorhanden“.

## Consequences
- Weil die Selbstregistrierung in jeder Versammlung möglich ist, kann jeder angemeldete Nutzer die Admin-Namen einer Versammlung sehen, indem er sich dort registriert. Es sind nur Vor- und Nachname.
- Änderungen direkt in der Firebase-Konsole ändern die Kontakte nicht mit; die Anmeldung der Admins berichtigt das nur für sie selbst. Ein degradierter Admin, dessen Kontakt nicht gelöscht werden konnte, kann ihn selbst löschen.
- Eine Cloud Function wäre robuster, brächte aber eigene Infrastruktur mit; sie lässt sich später ergänzen, ohne die Rules zu ändern.
