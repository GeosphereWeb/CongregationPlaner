# ADR-0032: Familie als Felder im Mitgliedsdokument

## Status
Accepted

## Context
Der Versammlungsadmin soll Mitglieder zu Familien zusammenfassen; jede Familie hat ein Familienoberhaupt, die übrigen Mitglieder stehen in einer Beziehung zu ihm. Freie Beziehungen zwischen beliebigen Mitgliedern werden nicht gebraucht. Eine eigene Collection für Familien hätte eigene Rules, Repositories und Konsistenzprobleme beim Entfernen von Mitgliedern gebracht.

## Decision
- Das Mitgliedsdokument trägt `familyHeadId` (uid des Oberhaupts) und `relationToHead` (`SPOUSE`, `CHILD`, `OTHER`); beide sind gemeinsam gesetzt oder `null`. Oberhaupt ist, wer von anderen genannt wird; es nennt selbst niemanden, Ketten gibt es nicht.
- `familyHeadCandidates` bestimmt die wählbaren Oberhäupter; `UpdateMemberProfileUseCase` und `CreateMemberWithoutAccountUseCase` prüfen die Wahl gegen die aktuellen Mitglieder. Die Rules prüfen nur Typ und Werte, weil sie andere Mitgliedsdokumente nicht bezahlbar prüfen können.
- `RemoveMemberUseCase` stellt die Familie vor dem Entfernen um: ohne neues Oberhaupt löst sie sich auf, beim Verknüpfen mit einem Konto geht sie auf das Konto über.

## Consequences
- Keine neue Collection und keine neuen Rules-Funktionen außer der Feldprüfung; die Familie kommt mit der Mitgliederliste mit.
- Ein Oberhaupt muss erst seine Familie auflösen, bevor es selbst einem anderen zugeordnet werden kann.
- Die Rules verhindern keine Verweise auf nicht vorhandene oder verkettete Oberhäupter; das sichert nur die App.
- Mitglieder sehen ihre Familie nicht, weil sie fremde Mitgliedsdokumente nicht lesen dürfen.
