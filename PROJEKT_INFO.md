# 🏗️ CongregationPlaner - KMP Struktur & Lernhilfe

Willkommen zu deinem Kotlin Multiplatform (KMP) Projekt! Diese Datei hilft dir dabei, die Struktur deines Projekts zu verstehen und zu lernen, wie KMP funktioniert.

## 📁 Modul-Übersicht

Dein Projekt ist in verschiedene Module unterteilt, um Code so weit wie möglich zu teilen, aber dennoch plattformspezifische Eigenheiten zu erlauben.

### 1. `shared` (Das Herzstück)
Dies ist das wichtigste Modul. Hier liegt die gesamte Geschäftslogik und (dank Compose Multiplatform) auch der Großteil der Benutzeroberfläche.
*   **`commonMain`**: Hier schreibst du Code, der auf **allen** Plattformen läuft. Er darf keine direkten Android- oder iOS-APIs verwenden.
*   **`androidMain`, `iosMain`, `jvmMain` (Desktop), `jsMain`, `wasmJsMain`**: Hier liegt Code, der Zugriff auf die jeweilige Plattform benötigt.

### 2. Plattform-Wrapper (`androidApp`, `iosApp`, `desktopApp`, `webApp`)
Diese Module sind oft sehr klein. Sie dienen hauptsächlich als "Starter" für die jeweilige Plattform:
*   **`androidApp`**: Die klassische Android-App. Sie initialisiert die App und ruft den Code aus dem `shared`-Modul auf.
*   **`desktopApp`**: Enthält die `main`-Funktion für Windows/macOS/Linux.
*   **`iosApp`**: Ein Xcode-Projekt, das das `shared` Framework einbindet.

---

## 🛠️ Der `expect` / `actual` Mechanismus

Das ist das wichtigste Konzept in KMP, um plattformspezifische Funktionen im gemeinsamen Code zu nutzen.

### Das Prinzip
1.  **`expect` (in `commonMain`)**: Du definierst eine "Erwartung". Du sagst: "Ich brauche eine Funktion/Klasse, aber jede Plattform muss selbst entscheiden, wie sie implementiert wird."
2.  **`actual` (in `androidMain`, `iosMain`, etc.)**: Du lieferst die "tatsächliche" Implementierung für genau diese Plattform.

### Beispiel: Plattform-Informationen
In deinem Projekt haben wir das so gelöst:

1.  **Erwartung (`shared/src/commonMain/.../Platform.kt`)**:
    ```kotlin
    interface Platform {
        val name: String
        val isDesktop: Boolean
    }
    expect fun getPlatform(): Platform
    ```

2.  **Implementierung für Android (`shared/src/androidMain/.../AndroidPlatform.kt`)**:
    ```kotlin
    class AndroidPlatform : Platform {
        override val name: String = "Android ${Build.VERSION.SDK_INT}"
        override val isDesktop: Boolean = false
    }
    actual fun getPlatform(): Platform = AndroidPlatform()
    ```

3.  **Implementierung für Desktop (`shared/src/jvmMain/.../JVMPlatform.kt`)**:
    ```kotlin
    class JVMPlatform : Platform {
        override val name: String = "Java ${System.getProperty("java.version")}"
        override val isDesktop: Boolean = true
    }
    actual fun getPlatform(): Platform = JVMPlatform()
    ```

---

## 🚀 Hilfsklassen für sauberen Code

Um den Zugriff auf diese Plattform-Infos zu vereinfachen, haben wir ein **Hilfsobjekt** in `commonMain` erstellt:

```kotlin
object HostPlatform {
    val current: Platform by lazy { getPlatform() }
    val isDesktop: Boolean get() = current.isDesktop
}
```

**Vorteil:** Du kannst nun überall im `commonMain` (z.B. in deinen UI-Composables) einfach schreiben:
`if (HostPlatform.isDesktop) { ... }`

---

## 🎨 UI mit Compose Multiplatform

Dein Projekt nutzt **Compose Multiplatform**. Das bedeutet:
*   Du schreibst deine UI einmal in `commonMain`.
*   Das Layout passt sich automatisch an (z.B. unterschiedliche Navigation für Mobile vs. Desktop).
*   Die UI-Elemente sehen auf allen Plattformen gleich aus, verhalten sich aber wie native Apps.

## 💡 Konventionen in diesem Projekt
*   **Dateiname = Klassenname**: Jede Datei ist exakt nach der darin enthaltenen Hauptklasse benannt (z. B. `AndroidPlatform.kt` statt `Platform.android.kt`).
*   **Keine Plattform-Suffixe im Namen**: Wir nutzen keine Endungen wie `.android.kt` oder `.ios.kt`, wenn der Dateiname selbst (z. B. `IOSPlatform.kt`) bereits eindeutig ist. Das macht die Tabs in Android Studio übersichtlicher.
*   **Zentrale Verwaltung**: Alle Bibliotheken und Versionen werden zentral in der `gradle/libs.versions.toml` definiert.

Viel Spaß beim Lernen von KMP! 🚀
