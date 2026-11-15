# ADR-0035: Benachrichtigungen als Firestore-Sammlung, Abruf durch die App

## Status
Accepted

## Context
Vortragsplaner und Helfer sollen erfahren, wenn der andere bei einem Termin „Informiert“ ändert. Später sollen weitere Benachrichtigungen an alle oder an Gruppen gehen, und das Senden soll eine eigene, später je Art einschränkbare Berechtigung haben. Echter Push (FCM) braucht einen Server mit Dienstkonto; Cloud Functions gibt es nur im kostenpflichtigen Blaze-Tarif. Die App soll ohne Kosten im Spark-Tarif bleiben.

## Decision
- Jede Benachrichtigung ist ein Dokument in `…/congregation/{id}/notifications/{id}` mit `category`, `title`, `body`, `audience`, `senderUid` und `createdAt` (Serverzeit). Es gibt keinen Server: Die Android-App fragt im Hintergrund nach neuen Dokumenten und zeigt sie selbst an (Plan: `docs/benachrichtigungen-plan.md`, Schritt 2).
- `audience` enthält Gruppen (ADR-0033) und Rollen (`CongregationRole`) in einer Liste. Ihre Namen überschneiden sich nicht; Mitglieder fragen mit einem einzigen `array-contains-any` nach ihren Gruppen und Rollen. Die Rules lassen nur freigegebene Mitglieder lesen, an deren Gruppen oder Rollen das Dokument gerichtet ist, dazu den Absender.
- Die Berechtigung zum Senden gilt je `category` (`NotificationPermissionPolicy.canSend(user, category)`, gleich in den Rules `canSendNotification`). `TALK_PLANNING` dürfen Planer und Helfer senden, und nur an `['TALK_PLANNER', 'TALK_HELPER']`. `ANNOUNCEMENT` (Mitteilungen, CP-165) senden freigegebene Mitglieder mit der neuen Rolle `NOTIFICATION_SENDER` („Benachrichtigungen senden“) an beliebige Gruppen und Rollen; der Versammlungsadmin vergibt sie wie die anderen Rollen (ADR-0012), die Admin-Rolle allein berechtigt nicht. Eine spätere Einschränkung der Rolle auf einzelne Kategorien (z. B. ein Feld `notificationCategories` im Mitgliedsdokument) ergänzt nur Policy, Rules und Rollenvergabe.
- Die Meldung zu „Informiert“ schreibt die App nach dem Speichern des Termins (`NotifyConfirmedChangedUseCase`, Android und Desktop). Scheitert nur die Meldung, bleibt der gespeicherte Status gültig und die Meldung entfällt.
- Dokumente werden nur angelegt, nie geändert oder gelöscht.
- Erinnerungen, die sich aus den Daten ergeben, sind keine Dokumente: Die Erinnerung „Redner informieren“ (CP-167) berechnet jedes Gerät von Planer oder Helfer bei der Abfrage im Hintergrund selbst aus den Terminen der Woche (ab Montag bis zum Vortragstag, solange der Termin nicht „Informiert“ ist), einmal am Tag ab 8 Uhr. Ist der Termin informiert, entfernt das Gerät die Erinnerung bei der nächsten Abfrage.

## Consequences
- Keine Kosten und kein Server; dafür kommen Benachrichtigungen verzögert an (Hintergrundarbeit auf Android frühestens alle 15 Minuten).
- Ein Planer oder Helfer könnte eine Meldung schreiben, ohne den Status zu ändern. Die Rules begrenzen das auf diese Rollen, den festen Empfängerkreis und die Textlänge; der Termin zeigt immer den wahren Status.
- Alte Benachrichtigungen bleiben liegen, weil es keinen Löschlauf gibt.
- Ein späterer Wechsel auf Push (Cloud Function auf „Dokument angelegt“) ersetzt nur die Zustellung; Datenmodell und Berechtigung bleiben.
