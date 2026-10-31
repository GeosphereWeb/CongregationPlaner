# ADR-0009: Vortragsplanung getrennt von der Rolle Versammlungsadmin

## Status
Accepted

## Context
Bisher durften neben globalen Admins auch alle freigegebenen Versammlungsadmins Vorträge planen (ADR-0002). Versammlungsadmins verwalten aber Mitglieder und Metadaten der Versammlung, nicht zwingend die Vortragsplanung. Die Rechte sollen stärker getrennt sein, ein Admin soll trotzdem zugleich Planer sein können.

## Decision
- `TalkSchedulePermissionPolicy.canPlanTalks` und die Funktion `canPlanTalks` in `firestore.rules` erlauben nur globale Admins und freigegebene Mitglieder mit `UserPermission.PLAN_TALKS`. Die Rolle `CONGREGATION_ADMIN` allein genügt nicht.
- Ein Versammlungsadmin, der auch plant, erhält `PLAN_TALKS` wie jedes andere Mitglied.
- Versammlungsadmins dürfen weiterhin Mitglieder verwalten und die Metadaten ihrer eigenen Versammlung ändern (`CongregationPermissionPolicy.canEdit`, `update` in den Rules).
- Das gilt für alles, was an `canPlanTalks` hängt: Termine, Vortragshistorie und das Verzeichnis externer Redner.

## Consequences
- Bestehende Versammlungsadmins verlieren den Zugriff auf die Vortragsplanung, bis ihnen `PLAN_TALKS` zugewiesen wird.
- `firestore.rules` und App-Version müssen zusammen ausgerollt werden; sonst zeigt die App Planungsfunktionen, die der Server ablehnt, oder umgekehrt.
- ADR-0002 beschreibt den früheren Zustand, in dem Versammlungsadmins planen durften.
