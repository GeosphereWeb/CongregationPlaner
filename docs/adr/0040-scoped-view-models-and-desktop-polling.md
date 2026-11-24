# ADR-0040: Bereichs-ViewModels enden mit dem Bereich, Desktop fragt sparsam ab

## Status
Accepted

## Context
Am 2026-11-24 war das Tageskontingent der Cloud-Firestore erschöpft, die Apps bekamen keine Daten mehr. Die Bereiche erzeugten ihr ViewModel per `remember { createGraph… }`; so ein ViewModel wird nie beendet, und verlassene Bereiche fragten weiter ab. Die Desktop-App (REST, ADR-0010) liest bei jeder Abfrage ganze Sammlungen und fragte alle 15 Sekunden ab, auch im Hintergrund.

## Decision
- Bereiche holen ihr ViewModel über `rememberScopedViewModel` (`presentation/components/ScopedViewModel.kt`). Es liegt in einem eigenen `ViewModelStore`, der beim Verlassen des Bereichs geleert wird; das beendet `viewModelScope` und damit alle Abfragen. Nur die app-weiten ViewModels in `App.kt` bleiben bei `remember`.
- Die Desktop-App fragt alle 60 Sekunden ab, sofort nach jedem eigenen Schreibzugriff und nur, solange das Fenster den Fokus hat; kommt der Fokus zurück, fragt sie sofort ab (`DesktopPolling`, `pollingFlow`).
- Daten, die nur ein Dialog braucht, werden beim Öffnen einmal gelesen statt laufend abgefragt; dieselbe Sammlung wird in einem Bereich nur einmal abonniert.

## Consequences
- Änderungen anderer Nutzer erscheinen auf dem Desktop erst nach bis zu 60 Sekunden; eigene Änderungen sofort.
- Wer einen Bereich verlässt und zurückkehrt, bekommt einen frischen Zustand (wie bisher, weil `remember` mit dem Bereich verloren ging).
- Die Startseite bleibt app-weit und fragt weiter ab, solange das Fenster aktiv ist.
