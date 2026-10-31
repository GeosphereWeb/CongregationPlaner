# ADR-0010: Nachtblau-Design als Tokens in `theming` und Bausteine in `uilib`

## Status
Accepted

## Context
Das Login-Design (Boards „Login-Ablauf“ und „Farbpalette“) braucht eigene Farben, zwei Schriften (Space Grotesk, JetBrains Mono) und wiederverwendbare Bausteine. `AppTheme` nutzt `MaterialTheme` derzeit nicht, ein helles Design ist noch offen.

## Decision
- Farben und Schriften liegen als Tokens in `theming` (`NightColors`, `nightTextStyle`); die Schriften sind statische TTFs als Compose-Ressourcen (SIL OFL, Lizenztexte in `theming/licenses`), weil Variable Fonts auf iOS und Desktop die Gewichte nicht zuverlässig wechseln.
- Die Bausteine (`TeleprompterHeader`, `NightPanel`, `NightScreen`, `FieldBox`, `OrgLevelRow`, `PrimaryButton`, `SecondaryButton`, `StatusChip`) liegen in `uilib` und lesen die Tokens direkt, nicht über `MaterialTheme`.
- Zunächst gibt es nur das dunkle Design. Die Material-Buttons des alten Designs (`PrimaryButtonComposable`) sind ersetzt.

## Consequences
- Ein helles Design braucht später entweder eine zweite Token-Menge oder die Anbindung an `MaterialTheme`; die Bausteine müssten dann nicht mehr direkt auf `NightColors` zugreifen.
- Die Schriften erhöhen die App-Größe um etwa 0,6 MB.
- Bildschirme außerhalb des Logins nutzen weiter das alte Aussehen, bis sie umgestellt werden.
