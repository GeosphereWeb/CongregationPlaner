# ADR-0031: Geschlecht statt Gruppe im Mitgliedsprofil

## Status
Accepted

## Context
Das Mitgliedsprofil (ADR-0016) speicherte die Gruppe `category` (Bruder, Schwester, Kind) neben `baptized`. Beides überschnitt sich: „Bruder“ und „Schwester“ bezeichnen getaufte Mitglieder, ließen sich aber auch ungetauft erfassen, und „Kind“ sagte nichts über das Geschlecht. Redner konnte jeder Bruder sein, auch ohne Dienstamt.

## Decision
- Das Profil speichert `gender` (`MALE`, `FEMALE` oder `null`, solange nicht gepflegt) statt `category`. Bruder und Schwester werden nicht gespeichert: `MemberProfile.isBrother` und `isSister` ergeben sich aus `gender` und `baptized`. Die Gruppe „Kind“ entfällt.
- Ein Dienstamt haben nur Brüder; Redner sind nur Dienstamtgehilfen und Älteste (`canBeSpeaker`). `normalized()`, `UpdateMemberProfileUseCase` und die Rules (`isValidProfile`) setzen dieselben Regeln durch.
- Das Rednerverzeichnis kennt kein Dienstamt: Ein von dort übernommener Redner wird als Ältester angelegt, der Admin korrigiert es bei Bedarf.

## Consequences
- Ein Mitglied kann nicht mehr widersprüchlich erfasst werden (z. B. ungetaufter Bruder).
- Ob jemand ein Kind ist, weiß die App nicht mehr; das ergibt sich künftig höchstens aus der Familienbeziehung.
- Übernommene Redner können fälschlich als Älteste geführt sein, bis der Admin sie korrigiert.
- Vorhandene Testdaten mit `category` sind ungültig und werden neu angelegt (keine Migration).
