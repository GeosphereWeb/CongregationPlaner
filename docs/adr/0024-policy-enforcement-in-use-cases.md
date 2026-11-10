# ADR-0024: Policies setzt der UseCase durch, die UI liest Rechte aus dem Zustand

## Status
Accepted

## Context
Die Policies (`…PermissionPolicy`) wurden uneinheitlich angewendet: Die Vortragsplanung und die Organisation prüften im UseCase, Nutzerverwaltung und Versammlungsdaten nur im ViewModel. Die Nutzerverwaltung bot zusätzlich `can…`-Funktionen am ViewModel an, die die Composables für jede Zeile aufriefen. Damit hing die Sicherheit einer Schreibaktion davon ab, welcher Aufrufer den UseCase nutzt.

## Decision
- Jeder schreibende UseCase bekommt den angemeldeten `AppUser` und prüft seine Policy selbst; ohne Recht liefert er `Result.failure` mit der `NotAllowed`-Exception des Fachbereichs (neu: `MemberProfileException.NotAllowed`, `CongregationException.NotAllowed`). Die Versammlung der Nutzerverwaltung kommt aus `user.congregation`, nicht vom Aufrufer.
- ViewModels nutzen die Policies nur noch, um die UI zu steuern. Die Rechte stehen als Datenklasse im `UiState` (z. B. `CongregationPermissions`, `MemberPermissions` je Mitglied), standardmäßig ist alles verboten. Composables rufen keine Policy und keine `can…`-Funktion auf.
- Regeln, die Rechte begründen, gehören in die Policy, z. B. warum die Admin-Rolle bestehen bleibt (`UserPermissionPolicy.adminRoleLock`). `AppUser.hasRole` fasst „freigegeben und hat die Rolle“ zusammen.
- Jede Policy liegt im Paket ihres Fachbereichs (`<feature>/domain/permission`). Aufgaben mit unterschiedlichen Rechten sind getrennte UseCases: Freigabe und Rolle (`SetMemberApprovalUseCase`, `SetMemberRoleUseCase`) sowie Anlegen durch den globalen Admin und Gründen der eigenen Versammlung im Login (`CreateCongregationUseCase`, `CreateOwnCongregationUseCase`).
- Die verbindliche Durchsetzung bleibt in `firebase/firestore.rules`; `RolePermissionMatrixTest` hält die Policies als Tabelle Rolle × Berechtigung fest.

## Consequences
- Jede Schreibaktion ist unabhängig vom Aufrufer geschützt; ViewModels und Import können keine Prüfung mehr vergessen.
- UseCases brauchen den Nutzer als Parameter, auch wenn die UI das Recht schon geprüft hat; einige Prüfungen laufen dadurch doppelt.
- Den Schutz der letzten Admin-Rolle (`AdminRoleLock.LAST_ADMIN`) gibt es nur in der App, die Rules kennen ihn nicht.
