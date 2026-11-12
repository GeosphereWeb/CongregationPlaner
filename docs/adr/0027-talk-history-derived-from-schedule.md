# ADR-0027: Vortragshistorie aus den Terminen berechnen

## Status
Accepted (ersetzt ADR-0007)

## Context
ADR-0007 hielt die Historie in einer eigenen Subcollection `talkHistory`, die `SyncTalkHistoryWithSlotUseCase` nach jedem Speichern nachzog. Der Redner stand dort nur als Namenstext, Termin und Historie konnten auseinanderlaufen, und je Vortrag passten höchstens 100 Tage hinein. Der Planungsassistent (`docs/vortragsplanung-assistent-plan.md`) braucht Redner und Vortrag je Tag mit ID, auch für nachgetragene Tage.

## Decision
- Die Historie ist keine gespeicherte Sammlung mehr. `talkHistoryOf` berechnet sie aus allen Terminen (`TalkScheduleRepository.observeAll`): je Vortragsnummer die Tage, ein abgesagter Vortrag zählt nicht, ein geplanter Termin in der Zukunft schon.
- Nachgetragen wird ein Tag als normaler Termin in der Vergangenheit, in der Planung (Jahr wählen, Termin anlegen). Er trägt damit Redner-ID oder externen Redner wie jeder andere Termin.
- `talkHistory`, der Abgleich, die Save/Delete-UseCases, der Historie-Teil des Imports und die Regeln dafür entfallen. Die Regeln lassen `talkHistory` nicht mehr zu; vorhandene Daten werden gelöscht (keine Migration, Testumgebung).

## Consequences
- Eine Quelle für Planung, Historie und spätere Vorschläge; kein Abgleich, der fehlschlagen kann.
- Die Historie-Ansicht liest alle Termine der Versammlung (etwa 52 je Jahr) statt eines Dokuments je Vortrag; für die erwarteten Mengen unkritisch.
- Pro Tag gibt es genau einen Termin, also lässt sich nur ein Vortrag je Tag nachtragen.
