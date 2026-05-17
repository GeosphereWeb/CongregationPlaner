# GitHub Workflows - Kotlin Multiplatform Übersicht

## 📊 Aktive Workflows in deinem Projekt

```
.github/workflows/
├── codeql.yml              ← Sicherheitsanalyse (Kotlin/Java spezialisiert)
├── ci.yml                  ← SonarCloud, Linting, Coverage
├── mergeable.yml           ← PR Compliance Check
└── WIP.yml                 ← Work-in-Progress Detection
```

## 🔄 Workflow Execution Flow

### Bei PUSH zu `main` oder `master`:
```
1. CodeQL läuft           (wenn master geändert)
2. CI läuft parallel      (Linting → Detekt → Coverage → SonarCloud)
3. Compliance Check       (Skipped, nur für PRs)
```

### Bei PULL REQUEST:
```
1. CI läuft               (SonarCloud Scan mit Coverage)
2. Compliance Check läuft (Titel + Commit validieren)
3. WIP Check läuft        (Verhindert "WIP" PRs)
4. CodeQL läuft           (Wenn auf master gemergt wird)
```

### Scheduled (Wöchentlich):
```
- CodeQL läuft jeden Donnerstag um 15:22 UTC
  (Zusätzliche Sicherheitsanalyse unabhängig von Commits)
```

## 🎯 KMP-Optimierungen

### CodeQL Build
```gradle
./gradlew clean :composeApp:compileKotlinJvm :composeApp:compileDebugKotlin
```
- **JVM-Target** explizit kompiliert
- **Debug-Variante** für vollständige Analyse
- **Clean-Build** für konsistente Ergebnisse

### CI Build
```gradle
./gradlew :composeApp:compileKotlinJvm        # JVM für SonarCloud
./gradlew koverXmlReport                       # Coverage-Reports
./gradlew detekt                               # Statische Analyse
./gradlew ktlintCheck                          # Linting
./gradlew sonar                                # SonarCloud Upload
```

## ✨ Neue Features in dieser Konfiguration

### 1. **Separate CodeQL**
- ❌ Kein doppeltes Kompilieren mehr
- ✅ Bessere Performance
- ✅ Java/Kotlin spezialisiert

### 2. **Enhanced CI**
- ✅ Gradle Cache für schnellere Builds
- ✅ `chmod +x gradlew` explizit für CI
- ✅ Codecov Integration (optional)
- ✅ No-daemon & No-build-cache Flags

### 3. **Branch Support**
- ✅ Beide `main` und `master` werden unterstützt
- ✅ Flexible für Migration neue Projekte

### 4. **Optimierte JDK**
- ✅ `temurin` Distribution (Open Source)
- ✅ Gradle Cache aktiviert
- ✅ JDK 21 für moderne Kotlin Features

## 🚀 Schnellstart

### 1. GitHub Secrets setzen
```bash
Gehe zu: Settings → Secrets and variables → Actions

Benötigte Secrets:
- SONAR_TOKEN        (von sonarcloud.io)
```

### 2. Test lokal (vor dem Push)
```bash
# Schneller Test
./gradlew ktlintCheck detekt

# Vollständiger Test (wie in CI)
./gradlew detekt ktlintCheck :composeApp:compileKotlinJvm koverXmlReport
```

### 3. Verfolge das Dashboard
```
CodeQL:     https://github.com/GeosphereWeb/CongregationPlaner/security/code-scanning
SonarCloud: https://sonarcloud.io/dashboard?id=GeosphereWeb_CongregationPlaner
Actions:    https://github.com/GeosphereWeb/CongregationPlaner/actions
```

## 📈 Metriken pro Workflow

| Metrik | Workflow | Ziel |
|--------|----------|------|
| **Security Issues** | CodeQL | 0 (Critical) |
| **Code Smells** | CI (Detekt) | < 5 |
| **Code Style** | CI (ktlint) | 0 Warnings |
| **Coverage** | CI (Kover) | > 50% |
| **Bugs** | SonarCloud | 0 (Critical) |
| **Duplicates** | SonarCloud | < 3% |
| **Naming Convention** | Compliance | CP-XXX Format |

## 🔧 Manuelle Triggers

### CodeQL neu starten (wenn nötig)
```
GitHub → Security → Code scanning → Re-run CodeQL
```

### CI neu starten
```
GitHub → Actions → CI → Re-run jobs
```

## ⚠️ Bekannte Besonderheiten für KMP

1. **Detekt + ktlint**: Läuft nur auf JVM, nicht auf native/iOS Targets
2. **Coverage**: Nur über Kover verfügbar (nicht über native Targets)
3. **CodeQL**: Build Fehler sind normal auf nativen Targets → deshalb nur JVM kompiliert

## 📚 Ressourcen

- [GitHub CodeQL Docs](https://codeql.github.com/)
- [SonarCloud für Kotlin](https://docs.sonarcloud.io/)
- [Detekt für KMP](https://detekt.dev/)
- [Kover für Multiplatform](https://github.com/Kotlin/kotlinx-kover)

---
**Für dein KMP-Projekt optimiert** | Version: 2026-05-17

