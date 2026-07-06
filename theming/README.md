# Theming Module

Das `theming`-Modul ist ein gemeinsames Kotlin Multiplatform-Modul für zentrale Theming-Definitionen und wird von allen Anwendungen (Android, Desktop, Web, iOS) verwendet.

## Struktur

- **AppTheme** - Zentrale Theme-Verwaltung mit Light/Dark Mode Support
- **AppColors** - Zentrale Farbdefinitionen
- **AppDimensions** - Spacing, Corner Radius, Icon-Größen und andere Dimensionen

## Verwendung

### In commonMain Code:

```kotlin
import de.geosphere.congregationplaner.theming.AppTheme
import de.geosphere.congregationplaner.theming.AppColors
import de.geosphere.congregationplaner.theming.AppDimensions

@Composable
fun MyScreen() {
    val isDarkMode = isSystemInDarkTheme()
    val colorScheme = AppTheme.colorScheme(isDarkMode)
    
    MaterialTheme(colorScheme = colorScheme) {
        Surface(
            color = AppColors.Background,
            modifier = Modifier.padding(AppDimensions.PaddingMedium)
        ) {
            Text("Hello Theme!")
        }
    }
}
```

## Abhängigkeiten

Das Modul hängt ab von:
- Compose Multiplatform UI
- Compose Material 3
- Kotlin Standard Library

Das Modul wird von allen App-Modulen benötigt und sollte als gemeinsame Quelle für Theme-Einstellungen verwendet werden.

## Testing

Tests sind im `src/commonTest` Verzeichnis lokalisiert und können mit:

```bash
./gradlew :theming:jvmTest
```

ausgeführt werden.
