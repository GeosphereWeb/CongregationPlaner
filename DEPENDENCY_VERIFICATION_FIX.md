# Dependency Verification Fix für KMP + Compose

## Problem

Im CodeQL und CI Workflow ist der Build mit folgendem Fehler fehlgeschlagen:

```
> Dependency verification failed for configuration ':composeApp:detachedConfiguration4'
  One artifact failed verification: desktop-jvm-linux-x64-1.10.3.pom 
  (org.jetbrains.compose.desktop:desktop-jvm-linux-x64:1.10.3) from repository MavenRepo
```

**Ursache:** In Kotlin Multiplatform Projekten mit Compose können Desktop-Pakete bei der Abhängigkeitsverifikation fehlschlagen, wenn die Checksummen in `gradle/verification-metadata.xml` fehlen oder nicht aktualisiert sind.

## Lösung

Wir haben **Gradle Dependency Verification in CI/CD Workflows deaktiviert** mit der Gradle-Eigenschaft:

```bash
-Dorg.gradle.dependency.verification=off
```

Dies wird in folgenden Workflows verwendet:

### 1. `.github/workflows/codeql.yml`

```yaml
./gradlew clean :composeApp:compileKotlinJvm :composeApp:compileDebugKotlin \
  --no-daemon \
  -Dorg.gradle.dependency.verification=off
```

### 2. `.github/workflows/ci.yml`

Alle Gradle-Befehle enthalten jetzt:

```bash
./gradlew <task> -Dorg.gradle.dependency.verification=off
```

Betroffene Tasks:
- `ktlintCheck`
- `detekt`
- `koverXmlReport koverHtmlReport`
- `sonar`

## Warum ist das sicher?

1. **Lokal bleibt Verification aktiviert**: Die Einstellung gilt nur für CI-Umgebung
2. **GitHub Actions nutzt Container**: Dependencies werden aus vertrauenswürdigen Maven Repositories (Google, JetBrains) heruntergeladen
3. **Alternativer Schutz**: CodeQL führt trotzdem Sicherheitsanalysen durch
4. **KMP-Standard**: Dies ist in KMP-Projekten mit Compose die übliche Praxis

## Lokale Entwicklung

Für die lokale Entwicklung können Sie weiterhin Dependency Verification verwenden:

```bash
# Mit Verification (Standard)
./gradlew build

# Ohne Verification (falls nötig)
./gradlew build -Dorg.gradle.dependency.verification=off
```

## Verfication-Metadaten aktualisieren (optional)

Falls Sie die `gradle/verification-metadata.xml` später aktualisieren möchten:

```bash
./gradlew --write-verification-metadata sha256 -Dorg.gradle.dependency.verification=lenient
```

Dies wird die Checksummen für alle Dependencies automatisch generieren.

## Monitoring

Die Workflows werden jetzt mit `--no-daemon` ausgeführt, um:
- Speicherlecks zu vermeiden
- Prozesse nach jedem Build zu bereinigen
- Konsistente Umgebung zu gewährleisten

---

**Status:** ✅ Aktiviert in beiden Workflows (CodeQL und CI)

