# ADR-0012: Eine Liste gleichrangiger Rollen je Mitglied

## Status
Accepted

## Context
Ein Mitglied hat bisher genau eine Rolle (`UserRole`: Versammlungsadmin oder Mitglied) und zusätzlich Einzelberechtigungen (`UserPermission`: `PLAN_TALKS`, `VIEW_MEMBERS`). Seit ADR-0009 gibt die Admin-Rolle keine Planungsrechte mehr; fachlich sind Admin und Planer gleichrangige Aufgaben, die man kombiniert. Zwei Begriffe für dasselbe verwirren in der Benutzerverwaltung, in den Policies und in `firestore.rules`. Für die Planung unter der Woche fehlt außerdem eine eigene Zuständigkeit.

## Decision
- Jedes Mitglied hat eine Liste von Rollen (`CongregationRole`) im Feld `roles` seines Mitgliedsdokuments: `CONGREGATION_ADMIN` (Versammlungsdaten, Userverwaltung), `TALK_PLANNER` (Vortragsplanung, ersetzt `PLAN_TALKS`) und `TASK_PLANNER` (Planung unter der Woche). Die Felder `role` und `permissions` entfallen; ohne Rolle ist man einfaches Mitglied.
- Jede Planerrolle darf die Mitglieder der eigenen Versammlung samt Kontaktdaten lesen; `VIEW_MEMBERS` entfällt.
- Admins dürfen ihre eigene Rollenliste ändern, aber nicht die eigene Admin-Rolle entfernen. Die App verhindert zusätzlich, dass eine Versammlung ihren letzten Admin verliert.
- Bis zur Bereinigung lesen App und Rules auch die alten Felder. Die App schreibt nur noch `roles` und stellt Altdokumente um, wenn ein Admin die Mitgliederliste lädt. Der globale Admin (`admins/{uid}`) bleibt außerhalb der Liste, weil er an keine Versammlung gebunden ist.

## Consequences
- Jeder Planer sieht Telefonnummern und E-Mail-Adressen aller Mitglieder seiner Versammlung; eine feinere Trennung kann später als eigene Rolle folgen.
- Altdokumente werden erst umgestellt, wenn ein Admin der Versammlung die Mitgliederliste öffnet; bis dahin tragen Rules und Mapper eine Rückfalllogik mit, die ein eigener Schritt wieder entfernt.
- `firestore.rules` und App müssen zusammen ausgerollt werden; ältere App-Versionen können sich danach nicht mehr selbst registrieren.
- Den letzten Admin können die Rules nicht erkennen; dieser Schutz liegt nur in der App.
- Ersetzt die Berechtigung `VIEW_MEMBERS` aus ADR-0004, `PLAN_TALKS` aus ADR-0009 und die Admin-Erkennung über `role` aus ADR-0011.
