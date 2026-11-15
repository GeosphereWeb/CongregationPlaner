# Umsetzungsplan: Benachrichtigungen

Stand: 2026-11-15. Gewählt ist Variante B: ohne Server, ohne Kosten (Spark-Tarif). Schritt 1 umgesetzt (CP-163,
ADR-0035), Schritt 2 umgesetzt (CP-164). Abweichung in Schritt 2: Die App fragt die Benachrichtigungs-Erlaubnis
beim Start statt erst nach der Freigabe; Android fragt ohnehin höchstens zweimal.

## Ziel

- Ändert ein Vortragsplanungs-Helfer bei einem Termin den Status „Informiert“, bekommt der Vortragsplaner eine
  Benachrichtigung auf dem Handy. Ändert der Planer den Status, bekommt sie der Helfer.
- In der Statuszeile erscheint `ic_stat_congregation`, in der Benachrichtigungsliste zusätzlich das App-Icon.
- Die Architektur trägt später weitere Benachrichtigungen an alle oder an eine eingeschränkte Gruppe. Das Versenden
  hängt an einer neuen Berechtigung, die sich später auf einzelne Kategorien einschränken lässt.

## Entscheidung: Variante B

Echter Push (FCM) braucht einen Server, und Cloud Functions gibt es nur im Blaze-Tarif mit Zahlungsmittel. Statt
dessen holt die App die Nachrichten selbst ab:

- Wer etwas mitteilt, schreibt ein Dokument in die Sammlung `notifications` der Versammlung.
- Jedes Android-Gerät fragt im Hintergrund regelmäßig nach neuen Dokumenten für seinen Nutzer und zeigt sie als
  Benachrichtigung an.

Folgen:

- Keine Kosten, kein Zahlungsmittel, keine Cloud Functions, kein FCM, keine Geräte-Tokens.
- Die Benachrichtigung kommt verzögert: Android erlaubt Hintergrundarbeit frühestens alle 15 Minuten und schiebt
  sie im Akkusparmodus weiter hinaus. Beim Öffnen der App wird sofort geprüft.
- Ein späterer Wechsel auf Push ersetzt nur die Zustellung (Function auf „Dokument angelegt“ statt Abfrage);
  Datenmodell, Berechtigung und Versand in der App bleiben.

## Ausgangslage im Code

- „Informiert“ ist das Feld `confirmed` in `…/congregation/{id}/talkSchedule/{yyyy-MM-dd}`. Der Helfer ändert es über
  `ConfirmSpeakerUseCase` → `TalkScheduleRepository.setConfirmed`, der Planer beim Speichern des ganzen Termins
  (ADR-0023). Geschrieben wird von Android (Firestore-SDK) und Desktop (`FirestoreRestTalkScheduleRepository`).
- Rollen stehen als Liste `roles` im Mitgliedsdokument (`CongregationRole`, ADR-0012); der Versammlungsadmin vergibt
  sie. Die Rules prüfen sie mit `hasRole` und `isValidRoles`.
- Gruppen für Empfänger gibt es schon bei den besonderen Ereignissen: `audience` mit `ALL` oder `BROTHERS`,
  `SISTERS`, `ELDERS`, `MINISTERIAL_SERVANTS`; Mitglieder fragen mit `array-contains-any` ihre Gruppen ab (ADR-0033).
- Das Manifest hat keine Benachrichtigungs-Berechtigung; `firebase/firebase.json` kennt keine Indizes.

## Architektur

```
Absender (Android/Desktop)                         Empfänger (Android)
  Status ändern ─┐                                   WorkManager alle 15 min + beim App-Start
                 ├─> notifications/{id} ──Abfrage──> neue Dokumente für meine Gruppen/Rollen
  Nachricht  ────┘   (Firestore)                     └─> lokale Benachrichtigung (beide Icons)
```

Es gibt einen einzigen Weg für alle Benachrichtigungen: ein Dokument in `notifications`. Die Statusänderung und
spätere Nachrichten an alle unterscheiden sich nur in Kategorie, Empfängerkreis und Berechtigung.

### Datenmodell

`…/congregation/{id}/notifications/{id}`:

- `category`: `TALK_PLANNING` (jetzt), später z. B. `ANNOUNCEMENT`. Bestimmt die nötige Berechtigung und den
  Android-Benachrichtigungskanal.
- `audience`: `ALL` oder eine Auswahl aus den Ereignis-Gruppen (ADR-0033) und den Rollen (`CongregationRole`).
- `title`, `body` (Längen begrenzt), `senderUid` (= `request.auth.uid`), `createdAt` (Serverzeit).
- Nur anlegen; ändern und löschen ist niemandem erlaubt.
- Lesen: freigegebene Mitglieder, wenn `audience` eine ihrer Gruppen oder Rollen enthält, sowie der Absender.
  Die App fragt wie bei den Ereignissen genau ihre Gruppen und Rollen ab, zusätzlich `createdAt >` letzter Stand.
  Dafür ist ein zusammengesetzter Index nötig (`firebase/firestore.indexes.json`, in `firebase.json` eintragen).

Für die Statusänderung: `category = TALK_PLANNING`, `audience = [TALK_PLANNER, TALK_HELPER]`, Text z. B.
„Redner informiert – So., 22.11.“ bzw. „Informiert zurückgenommen – So., 22.11.“. Der eigene Eintrag wird auf dem
Gerät des Absenders nicht angezeigt (`senderUid`).

### Berechtigung

Die Berechtigung zum Versenden gilt je Kategorie; das ist die Grundlage für die spätere Kaskade.

- `NotificationPermissionPolicy.canSend(user, category)`, durchgesetzt in `SendNotificationUseCase` (ADR-0024) und
  gleich in den Rules (`canSendNotification(category)`).
- `TALK_PLANNING`: Planer und Helfer (wer „Informiert“ ändern darf). Die Rules lassen für diese Kategorie nur den
  festen Empfängerkreis `[TALK_PLANNER, TALK_HELPER]` zu.
- Alle anderen Kategorien: neue Rolle `NOTIFICATION_SENDER` („Benachrichtigungen senden“) in `CongregationRole`.
  Sie nutzt die vorhandene Rollenvergabe (UI, Mapper, `isValidRoles`).
- **Kaskade später:** optionales Feld `notificationCategories: list<string>` im Mitgliedsdokument. Fehlt es, gilt
  die Rolle für alle Kategorien; ist es gesetzt, prüfen Policy und Rules zusätzlich `category in
  notificationCategories`. Dafür ändern sich dann nur die Policy, eine Rules-Funktion und die Rollenvergabe.

### Shared (KMP)

Neues Fachpaket `notification`:

- `domain/model`: `NotificationCategory`, `AppNotification` (mit `audience`)
- `domain/permission`: `NotificationPermissionPolicy`
- `domain/repository`: `NotificationRepository` (`send()`, `fetchSince(congregation, audiences, since)`)
- `domain/usecase`:
  - `SendNotificationUseCase` (prüft die Policy)
  - `NotifyConfirmedChangedUseCase` (baut Text und Empfängerkreis der Statusänderung); aufgerufen von
    `ConfirmSpeakerUseCase` und vom Speichern des Termins durch den Planer, wenn sich `confirmed` geändert hat.
    Scheitert das Benachrichtigen, bleibt die Statusänderung trotzdem gespeichert.
  - `FetchNewNotificationsUseCase` (leitet Gruppen und Rollen des Nutzers ab, lässt eigene Einträge weg)
- `data`: Android über das Firestore-SDK, Desktop über `FirestoreRestClient`. Der Desktop sendet nur (damit eine
  Änderung am Desktop den Helfer erreicht) und zeigt selbst keine Benachrichtigungen.

### Android

- Icon-Ordner `res` aus dem Download nach `androidApp/src/main/res`, Farbe `cp_primary` (#7A55B5), Manifest:
  `POST_NOTIFICATIONS`.
- `androidx.work` (WorkManager) über den Version Catalog. Ein periodischer Worker (15 Minuten, nur mit Netz) ruft
  `FetchNewNotificationsUseCase` auf; dieselbe Prüfung läuft beim Start der App.
- Der letzte Stand (`createdAt` der neuesten gesehenen Nachricht) liegt lokal auf dem Gerät. Beim ersten Start
  gilt „jetzt“, damit keine alten Nachrichten erscheinen.
- Anzeige mit `NotificationCompat`: `setSmallIcon(ic_stat_congregation)` für die Statuszeile,
  `setLargeIcon(App-Icon)` für die Liste, `setColor(cp_primary)`.
- Ein Benachrichtigungskanal je Kategorie. Der Nutzer schaltet damit jede Kategorie in den Android-Einstellungen
  stumm; eine eigene Einstellungsseite in der App entfällt.
- Antippen öffnet die App (`MainActivity`).
- Die Laufzeit-Erlaubnis (ab Android 13) wird nach der Freigabe des Mitglieds einmal abgefragt.

### Verbrauch

Jede Abfrage ohne Ergebnis zählt als ein Lesezugriff: rund 100 Lesezugriffe pro Gerät und Tag. Das Freikontingent
des Spark-Tarifs (50.000 Lesezugriffe pro Tag, Stand der Planung, vor dem Go-live prüfen) reicht damit für mehrere
hundert Geräte.

## Schritte (je ein Jira-Ticket und PR)

1. **Nachrichten-Sammlung und Statusänderung.** Paket `notification` (Modell, Policy, Repository für Android und
   Desktop, `SendNotificationUseCase`, `NotifyConfirmedChangedUseCase`), Aufruf an beiden Stellen, an denen
   `confirmed` geändert wird. Rules für `notifications` mit Kategorie `TALK_PLANNING`, Rules-Test (auch:
   Abfrage der eigenen Gruppen erlaubt, fremde abgelehnt), Tests für UseCases, Policy und Mapper. ADR
   „Benachrichtigungen als Firestore-Sammlung, Abruf durch die App“. Rules deployen, Emulator-Kopie aktualisieren.
2. **Zustellung auf Android.** Index für die Abfrage (`firebase/firestore.indexes.json`), Icon, Farbe, Manifest, Kanal, WorkManager-Worker, Prüfung beim App-Start, letzter
   Stand, Laufzeit-Erlaubnis, `FetchNewNotificationsUseCase` mit Tests. Abnahme mit zwei Geräten (Planer, Helfer).
3. **Berechtigung für weitere Kategorien.** Rolle `NOTIFICATION_SENDER` (Enum, Rules, Rollenvergabe), Policy und
   Rules für eine erste freie Kategorie (`ANNOUNCEMENT`) mit wählbarem Empfängerkreis, Rules-Test. Ohne
   Eingabemaske.

## Bewusst nicht enthalten

- Eingabemaske zum Verfassen einer Nachricht und ein Posteingang in der App (die Dokumente in `notifications`
  sind dafür schon die Grundlage)
- Einschränkung der Rolle je Kategorie (vorbereitet, siehe „Kaskade später“)
- Sofortige Zustellung (Push über FCM); nachrüstbar, siehe „Entscheidung“
- Aufräumen alter Nachrichten (ohne Server gibt es keinen automatischen Löschlauf)
- Sprung zum Termin beim Antippen
- iOS und Benachrichtigungen auf dem Desktop

## Risiken

- **Verzögerung und Hersteller-Akkusparen:** 15 Minuten sind das Minimum, nicht die Regel. Geräte von z. B. Xiaomi
  oder Huawei können Hintergrundarbeit stark drosseln; nach „Beenden erzwingen“ kommt bis zum nächsten Start der
  App nichts an.
- **Gefälschte Statusmeldung:** Ein Planer oder Helfer könnte technisch eine Meldung „informiert“ schreiben, ohne
  den Status zu ändern. Die Rules begrenzen das auf diese beiden Rollen, den festen Empfängerkreis und die
  Textlänge; der Termin selbst zeigt immer den wahren Status.
- **Mehrere Planer:** Alle Planer und Helfer außer dem Ändernden bekommen die Meldung, also auch ein zweiter
  Planer.
- **Worker und Anmeldung:** Der Worker läuft ohne geöffnete App und braucht die gespeicherte Firebase-Anmeldung
  und den DI-Graphen; das ist beim Umsetzen von Schritt 2 als Erstes zu prüfen.
