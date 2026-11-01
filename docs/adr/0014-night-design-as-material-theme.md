# ADR-0014: Nachtblau-Design als `MaterialTheme`

## Status
Accepted

## Context
ADR-0010 lässt die Bausteine in `uilib` direkt auf `NightColors` zugreifen. `AppTheme` setzt dabei kein `MaterialTheme`, deshalb erscheinen alle Material-Komponenten (Navigationsleiste, TopAppBar, Tabs, Cards, Switch, Dialoge) im Standard-Look.

## Decision
- `AppTheme` setzt ein `MaterialTheme`, dessen `ColorScheme`, `Typography` und `Shapes` aus den Night-Tokens abgeleitet sind (`nightColorScheme()`, `nightTypography()`, `nightShapes()`).
- Vorerst gibt es nur das dunkle Design, unabhängig von der Systemeinstellung. Der Parameter `useDarkTheme` und das helle Farbschema entfallen.
- Die Login-Bausteine aus ADR-0010 bleiben unverändert.

## Consequences
- Neue Oberflächen nutzen `MaterialTheme` statt `NightColors`.
- Ein helles Design braucht später nur ein zweites `ColorScheme`.
- Die Material-Standardkomponenten sehen ohne weitere Anpassung im Nachtblau-Design aus.
