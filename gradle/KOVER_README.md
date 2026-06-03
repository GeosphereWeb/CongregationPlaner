# Kover Coverage Configuration

## Übersicht

Kover ist ein Kotlin-Test-Coverage-Tool für Multiplatform-Projekte. Die Konfiguration für dieses Projekt ist zentral organisiert für einfache Wartung und Erweiterbarkeit.

## Projektstruktur

- **Konfigurationsdatei:** `gradle/kover.gradle.kts`
- **Dokumentation:** `gradle/KOVER_README.md` (diese Datei)
- **Module mit Kover:** 
  - `shared/` (KMP Library)
  - `androidApp/` (Android)
  - `desktopApp/` (Desktop/JVM)
  - `webApp/` (Web/JS/Wasm)

## Standard-Exclusions

Folgende Muster werden standardmäßig **ausgeschlossen** von der Coverage:

### Generated Code
```
**/generated/**
**/*\$*    (Inner/Companion-Klassen)
```

### Test Source Sets
```
**/test/**
**/Test*.kt
**/*Test.kt
**/*Tests.kt
**/androidTest/**
```

### Build Artifacts
```
**/build/**
```

## Coverage Reports generieren

### JVM/Desktop
```bash
./gradlew :shared:jvmTest koverReport
```

### Android
```bash
./gradlew :shared:testAndroidHostTest koverReport
```

### Merged Report (alle Module)
```bash
./gradlew koverMergedHtmlReport
./gradlew koverMergedXmlReport
```

### Mit SonarQube Integration
```bash
./gradlew sonarqube
```

## Reports anschauen

- **HTML:** `build/reports/kover/merged/html/index.html`
- **XML (SonarQube):** `build/reports/kover/merged/report.xml`

## Eigene Exclusions hinzufügen

### Modul-spezifische Exclusions

Um Exclusions für ein spezifisches Modul hinzuzufügen, erstelle einen `afterEvaluate`-Block in der entsprechenden `build.gradle.kts`:

```kotlin
// In desktopApp/build.gradle.kts oder webApp/build.gradle.kts
afterEvaluate {
    // Zusätzliche Kover-Konfiguration hier
    // (Abhängig von Kover-Version und verfügbarer API)
}
```

### Root-Exclusions

Für globale Exclusions bearbeite `gradle/kover.gradle.kts` und passe die Patterns an.

## Häufige Exclusion-Patterns (Referenz)

```kotlin
// DI / Framework Configuration
"**/di/**"
"**/config/**"
"**/di/**/*Config*.kt"

// UI Previews / Tooling
"**/*Preview*.kt"
"**/*Composable$$DefaultImpls"

// Data Models / DTOs
"**/model/**"
"**/dto/**"
"**/data/model/**"

// Sealed Classes Generierte Inner Classes
"**/*\$*"

// Test Doubles
"**/fake/**"
"**/mock/**"
"**/test/fixtures/**"

// Resources
"**/res/**"
"**/resources/**"
```

## Coverage Threshold konfigurieren

Mit Kover kannst du auch Mindestabdeckungen definieren (z.B. für CI/CD):

```kotlin
// In build.gradle.kts (wenn Kover API verfügbar)
kover {
    // Coverage rules konfigurieren (Kover 0.9.8+)
}
```

## Troubleshooting

### Problem: Reports werden nicht generiert
```bash
./gradlew clean koverReport -i
```

### Problem: zu viel Code wird ausgeschlossen
- Überprüfe die Patterns in `gradle/kover.gradle.kts`
- Nutze spezifischere Patterns (z.B. `**/generated/dagger/**` statt `**/generated/**`)

### Problem: Tests werden als Coverage gezählt
- Stelle sicher, dass Test-Source-Sets korrekt in Gradle definiert sind
- Überprüfe `gradle/kover.gradle.kts` auf die Test-Exclusion-Patterns

## Weitere Ressourcen

- [Kover GitHub Repository](https://github.com/Kotlin/kotlinx-kover)
- [Kover Konfigurationsdokumentation](https://github.com/Kotlin/kotlinx-kover/blob/main/docs/gradle-plugin/configuration.md)
- [Kotlin Multiplatform Coverage](https://kotlinlang.org/docs/multiplatform-get-started.html)


