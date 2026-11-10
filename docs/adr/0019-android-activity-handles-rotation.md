# ADR-0019: Android-Activity behandelt Drehung und Größenänderung selbst

## Status
Accepted

## Context
Beim Drehen des Handys startete die App auf Home, in jedem Bereich. Android erstellt die `MainActivity` bei einer Konfigurationsänderung neu. `App()` legt die ViewModels (Navigation, Nutzerzugriff, Versammlungsauswahl, Home) mit `remember` an, nicht über den `ViewModelStore` der Activity; ihr Zustand ist nach der Neuerstellung weg, auch der Navigationszustand und ungespeicherte Eingaben.

## Decision
`MainActivity` deklariert `android:configChanges` für Ausrichtung, Fenstergröße, Tastatur und Navigation (`AndroidManifest.xml`). Compose reagiert auf die neue Größe, ohne dass die Activity neu erstellt wird. Sprache, Dunkelmodus und Schriftgröße lösen weiter eine Neuerstellung aus.

## Consequences
- Drehen behält Bereich, Auswahl und Eingaben in allen Bereichen, ohne jedes ViewModel einzeln umzustellen.
- Das ist ein Workaround, keine Zustandssicherung: Ein Wechsel von Sprache, Dunkelmodus oder Schriftgröße und das Beenden des Prozesses durch das System setzen die App weiterhin auf Home zurück.
- Sollen auch diese Fälle den Zustand behalten, müssen die ViewModels über `viewModel { … }` an den `ViewModelStore` gebunden und der Navigationszustand gesichert werden; dann kann `configChanges` entfallen.
