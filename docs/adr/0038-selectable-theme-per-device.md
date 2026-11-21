# ADR-0038: Wählbares Design je Gerät

## Status
Accepted

## Context
Das Nachtblau-Design (ADR-0010, ADR-0014) wirkt mit lila Verlauf, Glow und Monospace-Labels wie eine generische KI-App und kennt nur einen dunklen Modus. Zwei sachliche Alternativen (Blau, Lila) nach Material 3 mit Hell und Dunkel sollen im Alltag verglichen werden, bevor eine davon zum Standard wird. Rund 30 Dateien lesen die Farben direkt aus `NightColors`, nicht aus `MaterialTheme`.

## Decision
- Unter Einstellungen → Allgemein → Darstellung wählt man Design (Nachtblau, Blau, Lila) und Modus (System, Hell, Dunkel). Die Wahl liegt nur auf dem Gerät (`KeyValueStorage`) und gilt sofort; ohne Wahl bleibt Nachtblau.
- `AppTheme` setzt das passende `ColorScheme` und schreibt eine daraus abgeleitete `NightPalette` in `NightColors.palette` (Snapshot-State). Die Tokens in `NightColors` lesen daraus, sodass auch direkte Zugriffe dem Design folgen; die sachlichen Designs nutzen die Systemschrift und keinen Header-Verlauf.
- Tokens dürfen nicht beim Klassen-Init festgehalten werden (z. B. als Enum-Konstruktorwert), sondern werden bei jedem Zugriff gelesen.

## Consequences
- Der globale Zustand in `NightColors` ist ein Provisorium: Previews und Tests teilen sich die aktive Palette. Nach der Entscheidung für ein Design werden die Bildschirme auf `MaterialTheme.colorScheme` umgestellt und dieser Mechanismus entfernt.
- Der Grundsatz „nur dunkel“ aus ADR-0014 gilt für die sachlichen Designs nicht mehr.
- Die Theme-Auswahl ist nicht an das Konto gebunden und wandert nicht auf andere Geräte mit.
