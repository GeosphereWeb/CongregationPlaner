# SonarCloud + Kover – Arbeitsstand (2026-06-24)

Diese Datei hält den aktuellen Erkenntnisstand fest, damit die Arbeit an Coverage/SonarCloud später fortgesetzt werden kann.

---

## Ausgangproblem

In SonarCloud wurde **keine Coverage** angezeigt für:

- `shared/src/commonMain/kotlin/de/geosphere/congregationplaner/Platform.kt`
- `shared/src/jsMain/kotlin/de/geosphere/congregationplaner/JsPlatform.kt`

Referenzen: `KOVER_SETUP.md`, `.github/copilot-instructions.md`

---

## Ursachen (identifiziert)

### 1. Kover misst nur JVM/Android

Kover 0.9.8 sammelt Coverage **nur** für JVM- und Android-Host-Tests.  
`jsMain`, `wasmJsMain`, `iosMain` werden **nicht** instrumentiert.

→ Code in `jsMain` erscheint nie im Kover-XML, solange er nur dort liegt.

### 2. CI lief ohne Tests vor dem Report

`.github/workflows/sonarcloud.yml` führte `koverXmlReport` aus, **bevor** Tests liefen.  
Ohne `:shared:jvmTest` und `:shared:testAndroidHostTest` liefert Kover keine brauchbaren Coverage-Daten.

### 3. Sonar kannte KMP-Source-Sets nicht zuverlässig

SonarCloud braucht explizite `sonar.sources` / `sonar.tests` für Pfade wie `commonMain`, `jvmMain`, …  
Sonst kann Coverage (package + Dateiname aus Kover-XML) nicht den Quelldateien zugeordnet werden.

### 4. Doppelte Quell-Indexierung (CI-Fehler #1)

**Fehlermeldung:**
```
File androidApp/src/main/kotlin/.../MainActivity.kt can't be indexed twice.
```

**Ursache:** `collectKotlinSourceDirs()` hat zunächst **alle** Ordner namens `kotlin` durchsucht – inkl. `androidApp/build/kotlin` (Build-Artefakt nach `assemble`).  
Damit wurde `MainActivity.kt` doppelt indexiert:

- `androidApp/build/kotlin/.../MainActivity.kt`
- `androidApp/src/main/kotlin/.../MainActivity.kt`

**Fix (bereits umgesetzt):** Pfadsammlung nur noch unter `src/<sourceSet>/kotlin`, keine `build/`-Ordner.

---

## Bereits umgesetzte Änderungen

| Datei | Änderung |
|-------|----------|
| `build.gradle.kts` | `collectKotlinSourceDirs()` nur für `src/*/kotlin`; `sonar.sources` / `sonar.tests`; `koverXmlReport` hängt von `jvmTest` / `testAndroidHostTest` / `test` ab |
| `shared/src/commonMain/.../JsPlatform.kt` | **Neu:** Browser-Erkennung + `JsPlatform(userAgent)` (Kover-kompatibel) |
| `shared/src/jsMain/.../JsPlatform.kt` | Nur noch `actual fun getPlatform()` mit `navigator.userAgent` |
| `shared/src/commonTest/.../PlatformTest.kt` | Tests für `detectBrowserName()` und `JsPlatform` |
| `shared/src/jsTest/.../JsPlatformTest.kt` | Smoke-Tests für JS-`getPlatform()` |
| `.github/workflows/sonarcloud.yml` | Tests vor Coverage: `:shared:jvmTest :shared:testAndroidHostTest koverXmlReport mergeKoverXml ...` |

### Lokaler Verifikationsstand ( erfolgreich )

```bash
gradlew.bat :shared:jvmTest :shared:testAndroidHostTest :shared:koverXmlReport mergeKoverXml
```

Im Report `build/reports/kover/merged/report.xml` sind enthalten:

- **`Platform.kt`** – Zeilen 32, 37 (`HostPlatform`) covered
- **`JsPlatform.kt`** (commonMain) – 7 Zeilen covered

---

## Offenes Problem: CI schlägt weiter fehl

### Fehler #1 (behoben)
Doppel-Indexierung von `MainActivity.kt` – Fix in `collectKotlinSourceDirs()` (nur `src/*/kotlin`, keine `build/`-Artefakte).

### Fehler #2 (behoben lokal)
Doppel-Indexierung durch Sonar-Gradle-Auto-Detect **plus** manuelle `sonar.sources`:

**Fix (2026-06-24):** Alle Unterprojekte mit `isSkipProject = true` überspringen; nur Root-Analyse mit expliziten KMP-Pfaden.

```kotlin
subprojects {
    sonar {
        isSkipProject = true   // Kotlin-DSL: skipProject ist private, Setter heißt isSkipProject
    }
}
```

**Lokaler Verifikationslauf:** Kein `can't be indexed twice` mehr; Scheitern nur noch erwartungsgemäß ohne `SONAR_TOKEN` (403 / „Not authorized“).

### Weitere Anpassungen (2026-06-24)

| Datei | Änderung |
|-------|----------|
| `build.gradle.kts` | `subprojects { sonar { isSkipProject = true } }`; Task-Abhängigkeiten `sonarqube` → `sonar` |
| `.github/workflows/sonarcloud.yml` | Ein Gradle-Schritt; `assemble` entfernt (Tests ziehen Build mit); `sonar` statt `sonarqube` |

### Lokaler Sonar-Lauf (ohne Token)

```bash
gradlew.bat ... sonar
```

Scheitert erwartungsgemäß mit:
```
HTTP 403 Forbidden ... check sonar.token or SONAR_TOKEN
```

→ CI braucht gültiges `SONAR_TOKEN` Secret; 403 wäre ein separater Konfigurationsfehler.

---

## Nächste Schritte (TODO)

1. ~~**`skipProject = true`** für alle `subprojects` testen~~ ✅ lokal mit `isSkipProject = true`
2. ~~Task `sonarqube` → `sonar` migrieren~~ ✅
3. ~~CI vereinfachen (kein doppeltes `assemble`)~~ ✅
4. **CI-Lauf abwarten** – prüfen, ob SonarCloud-Analyse grün durchläuft
5. In SonarCloud prüfen, ob Coverage für `Platform.kt` und `JsPlatform.kt` (commonMain) sichtbar ist

---

## Wichtige Befehle

```bash
# Tests + Coverage-Report (lokal)
gradlew.bat :shared:jvmTest :shared:testAndroidHostTest koverXmlReport mergeKoverXml

# Vollständiger Sonar-Lauf (CI / mit SONAR_TOKEN)
gradlew.bat :shared:jvmTest :shared:testAndroidHostTest koverXmlReport mergeKoverXml sonar

# Reports
# HTML:  build/reports/kover/merged/html/index.html
# XML:   build/reports/kover/merged/report.xml
```

---

## Architektur-Hinweis JsPlatform

| Pfad | Rolle | Kover |
|------|-------|-------|
| `commonMain/.../JsPlatform.kt` | Browser-Logik + Klasse | ✅ messbar via JVM/Android-Tests |
| `jsMain/.../JsPlatform.kt` | Nur `actual getPlatform()` | ❌ nicht messbar (Kover-Limitierung) |

Gleiches Prinzip gilt für `WasmPlatform.kt`, `IOSPlatform.kt` – plattformspezifische Stubs in nativen Source Sets sind ohne Kover-JS/Native-Support nicht coverbar.

---

## Versionen

- Kover: **0.9.8** (`gradle/libs.versions.toml`)
- SonarQube Gradle Plugin: **7.3.1.8318**
- SonarCloud: `geosphereweb` / `GeosphereWeb_CongregationPlaner`

---

**Status:** Coverage lokal OK; Sonar-Indexierung lokal ohne Duplikat-Fehler. Nächster Schritt: CI-Lauf + Coverage in SonarCloud verifizieren.
