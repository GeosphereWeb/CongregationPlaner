# Kover Exclusions - Implementierung Abgeschlossen ✅

## Was wurde eingerichtet:

### 1. **Zentrale Konfigurationsdatei**
   - **Datei:** `gradle/kover.gradle.kts`
   - **Zweck:** Zentrale Verwaltung aller Kover-Konfigurationen
   - **Importiert in:** Alle Module (shared, androidApp, desktopApp)

### 2. **Standard-Exclusions aktiviert**
   Die folgenden Patterns werden automatisch von der Coverage ausgeschlossen:

   ```
   ✓ Generated Code:          **/generated/**, **/*$*
   ✓ Test Source Sets:        **/test/**, **/Test*.kt, **/*Test.kt, **/*Tests.kt, **/androidTest/**
   ✓ Build Artifacts:         **/build/**
   ```

### 3. **Verfügbare Tasks**

   **Lokale Reports:**
   ```bash
   ./gradlew :shared:jvmTest koverReport
   ./gradlew :shared:testAndroidHostTest koverReport
   ```

   **Merged Reports (alle Module):**
   ```bash
   ./gradlew koverMergedHtmlReport      # HTML Report
   ./gradlew koverMergedXmlReport       # XML Report (für SonarQube)
   ./gradlew mergeKoverXml              # Fallback XML Merge
   ```

   **Mit SonarQube:**
   ```bash
   ./gradlew sonarqube                  # Lädt abhängig vom Merged XML Report
   ```

### 4. **Reports anschauen**
   - **HTML Dashboard:** `build/reports/kover/merged/html/index.html`
   - **XML für SonarQube:** `build/reports/kover/merged/report.xml`

---

## Modul-Struktur

```
CongregationPlaner/
├── gradle/
│   ├── kover.gradle.kts              ← Zentrale Konfiguration
│   ├── KOVER_README.md               ← Dokumentation & Referenz
│   └── libs.versions.toml            ← Kover 0.9.8 Version
├── shared/build.gradle.kts           ← apply(from = "kover.gradle.kts")
├── androidApp/build.gradle.kts       ← apply(from = "kover.gradle.kts")
├── desktopApp/build.gradle.kts       ← apply(from = "kover.gradle.kts")
└── KOVER_SETUP.md                    ← Diese Datei
```

---

## Nächste Schritte

### Wenn du zusätzliche Exclusions brauchst:

1. **Local Exclusion (per Modul):**
   - Bearbeite die jeweilige `build.gradle.kts` und nutze Kover DSL

2. **Global Exclusion (alle Module):**
   - Bearbeite `gradle/kover.gradle.kts` und passe die Patterns an

3. **Für detaillierte Konfiguration:**
   - Siehe `gradle/KOVER_README.md` für alle Patterns & Beispiele

### Häufigste Anpassungen:

```kotlin
// DI-Layer ausschließen
"**/di/**"

// UI-Previews ausschließen
"**/*Preview*.kt"

// Specific package ausschließen
"de.geosphere.congregationplaner.config.**"
```

---

## Troubleshooting

| Problem | Lösung |
|---------|--------|
| Kover-Tasks nicht sichtbar | `./gradlew clean && ./gradlew tasks \| grep kover` |
| Reports nicht generiert | Sorge für Tests: `./gradlew jvmTest` vor `koverReport` |
| Zu viel oder zu wenig Code in Report | Überprüfe `gradle/kover.gradle.kts` Exclusions |
| SonarQube findet Report nicht | Run `./gradlew sonarqube` (macht das Merge automatisch) |

---

## Ressourcen

- 📖 [Kover Dokumentation](https://github.com/Kotlin/kotlinx-kover)
- 📋 [Gradle Kover Plugin](https://plugins.gradle.org/plugin/org.jetbrains.kotlinx.kover)
- 🔍 [SonarQube + Kover Integration](https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-gradle/)

---

**Status:** ✅ Kover Exclusions erfolgreich konfiguriert
**Kover Version:** 0.9.8 (siehe `gradle/libs.versions.toml`)
**Last Updated:** 2026-06-04
