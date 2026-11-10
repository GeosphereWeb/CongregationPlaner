# ADR-0023: Rolle „Vortragsplanungs-Helfer“ sieht die Planung und markiert Redner als informiert

## Status
Accepted

## Context
Die Vortragsplanung hängt an der Rolle Vortragsplaner (`TALK_PLANNER`): Wer sie hat, sieht und ändert Termine, Rednerliste und Vortragshistorie. Ein Helfer soll den nächsten Termin und die Planung sehen und nur festhalten können, dass ein Redner informiert ist, ohne etwas anderes zu ändern. Er ruft die Redner an und braucht dafür ihre Nummern.

## Decision
- Neue Rolle `TALK_HELPER` („Vortragsplanungs-Helfer“), von Versammlungsadmins zuweisbar wie die anderen Rollen (ADR-0012). Sie ist unabhängig von den anderen Rollen und gilt nur für freigegebene Mitglieder.
- `TalkSchedulePermissionPolicy`: `canPlanTalks` bleibt dem Planer vorbehalten. Neu sind `canViewTalkPlanning` (Planer oder Helfer: Termine, Rednerliste, Vortragshistorie sehen) und `canConfirmSpeaker` (Planer oder Helfer: einen Redner als informiert markieren).
- Der Helfer ändert einen Termin nur über `ConfirmSpeakerUseCase`, das über `TalkScheduleRepository.setConfirmed` ausschließlich das Feld `confirmed` schreibt. Die Rules erlauben ihm im Termin nur diese Änderung (`affectedKeys().hasOnly(['confirmed'])`); Anlegen und Löschen sowie alle Änderungen an Rednerliste und Vortragshistorie bleiben dem Planer vorbehalten.
- Der Helfer darf **Mitglieder lesen** (`canViewMembers`), wie der Aufgabenplaner. Die Rules können beim Lesen keine Felder ausblenden; er sieht damit auch Adresse und Notfallkontakte (ADR-0016). Ihre Nummern braucht er für Anruf und WhatsApp (ADR-0020).

## Consequences
- Der Helfer sieht mehr personenbezogene Angaben, als er zum Anrufen braucht. Das ist dieselbe Abwägung wie beim Aufgabenplaner; eine Trennung bräuchte ein eigenes Dokument mit den Kontaktnummern.
- Die Oberfläche (Home, Navigation, Editor mit nur „Informiert“, Nur-Lesen-Ansichten) folgt in eigenen Änderungen. Bis dahin hat die Rolle auf dem Server und in der Fachlogik Wirkung, die Bildschirme aber noch nicht.
- Die Rules müssen nach dem Merge veröffentlicht werden, sonst lehnt die Online-Datenbank die neue Rolle ab.
