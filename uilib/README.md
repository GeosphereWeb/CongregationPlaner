# UI Library Module

Das `ui`-Modul ist eine gemeinsame Kotlin Multiplatform-Bibliothek mit wiederverwendbaren Compose UI-Komponenten für alle Anwendungen (Android, Desktop, iOS).

## Struktur

```
ui/src/commonMain/kotlin/de/geosphere/congregationplaner/ui/
├── button/           - Button-Komponenten
├── card/             - Card-Komponenten
├── appbar/           - Top App Bar Komponenten
├── dialog/           - Dialog-Komponenten
└── common/           - Allgemeine Komponenten (Loading, etc.)
```

## Komponenten

### Button Components (`button/`)

```kotlin
import de.geosphere.congregationplaner.ui.button.*

// Primary Button
PrimaryButton(
    text = "Speichern",
    onClick = { /* ... */ }
)

// Secondary Button
SecondaryButton(
    text = "Abbrechen",
    onClick = { /* ... */ }
)

// Outlined Button
OutlinedButton(
    text = "Mehr Info",
    onClick = { /* ... */ }
)
```

### Card Components (`card/`)

```kotlin
import de.geosphere.congregationplaner.ui.card.*

// Simple Card
AppCard {
    Text("Card content")
}

// Elevated Card (mit Schatten)
ElevatedAppCard {
    Text("Elevated card content")
}
```

### App Bar Components (`appbar/`)

```kotlin
import de.geosphere.congregationplaner.ui.appbar.*

// App TopBar mit Navigation
AppTopBar(
    title = "Mein Screen",
    onNavigationClick = { navController.popBackStack() }
)

// Simple TopBar ohne Navigation
SimpleAppTopBar(title = "Home")
```

### Dialog Components (`dialog/`)

```kotlin
import de.geosphere.congregationplaner.ui.dialog.*

// Confirm Dialog
ConfirmDialog(
    title = "Löschen?",
    message = "Willst du das wirklich löschen?",
    onConfirm = { /* delete */ },
    onDismiss = { /* cancel */ }
)

// Info Dialog
InfoDialog(
    title = "Info",
    message = "Das ist eine Informationsmeldung",
    onDismiss = { /* close */ }
)
```

### Common Components (`common/`)

```kotlin
import de.geosphere.congregationplaner.ui.common.*

// Loading Indicator
LoadingIndicator()

// Fullscreen Loading Overlay
FullscreenLoadingOverlay(isLoading = true)
```

## Abhängigkeiten

Das Modul hängt ab von:
- **theming** - Farben, Dimensionen, Typografie
- Compose Multiplatform UI, Foundation, Material 3, Animation
- Kotlin Standard Library

## Abhängigkeitsverwaltung

```
┌─────────────────────────────────────┐
│      App Modules                    │
│ (androidApp, desktopApp, iosApp)   │
└─────────────┬───────────────────────┘
              │
         ┌────▼─────┐
         │  shared/  │ (Business Logic)
         └────┬─────┘
              │
         ┌────▼──┐
         │  ui/  │ (UI Components)
         └────┬──┘
              │
         ┌────▼────────┐
         │  theming/   │ (Styles)
         └─────────────┘
```

## Testing

Tests können mit folgendem Befehl ausgeführt werden:

```bash
# JVM Tests
./gradlew :ui:jvmTest

# Alle Targets
./gradlew :ui:test
```

## Best Practices

1. **Komponenten sollten keine Business Logic enthalten** - verwende ViewModels in `shared/`
2. **Theming verwenden** - alle Farben und Dimensionen sollten über `AppTheme`, `AppColors`, `AppDimensions` kommen
3. **Modifier erlauben** - komponenten sollten einen `Modifier`-Parameter haben für Flexibilität
4. **Default-Werte setzen** - z.B. für Farben, Größen, Padding

Beispiel:

```kotlin
@Composable
fun MyComponent(
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppColors.Surface,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.background(backgroundColor)
    ) {
        content()
    }
}
```

## Erweitern

Um eine neue Komponente hinzuzufügen:

1. Erstelle eine neue Datei im entsprechenden Package
2. Implementiere die `@Composable` Funktion
3. Schreibe Tests in `src/commonTest/`
4. Exportiere die Komponente (optional: erstelle eine `index.kt` pro Package)
