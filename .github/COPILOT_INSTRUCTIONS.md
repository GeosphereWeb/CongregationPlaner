# Copilot instructions for CongregationPlaner

Purpose: Give Copilot-powered sessions quick, actionable knowledge about building, testing, and the repository architecture so suggestions are grounded in project conventions.

---

## Build, test, and lint commands (use Gradle wrapper)
- Use the project Gradle wrapper (Unix/macOS: `./gradlew`; Windows: `gradlew.bat`).
- Build all modules: `./gradlew assemble` (or `gradlew.bat assemble`).
- Android (debug APK): `./gradlew :androidApp:assembleDebug`.
- Desktop run / hot reload:
  - Hot reload: `./gradlew :desktopApp:hotRun --auto`
  - Run: `./gradlew :desktopApp:run`
- iOS: open `iosApp` in Xcode and run from the IDE (shared produces an iOS framework).

Testing (module/target-specific tasks from README):
- Android host tests: `./gradlew :shared:testAndroidHostTest`
- JVM/Desktop tests: `./gradlew :shared:jvmTest`
- iOS simulator tests: `./gradlew :shared:iosSimulatorArm64Test`
- Coverage: use Kover (Kotlin Kover) for test coverage reporting; run `./gradlew koverReport` and configure the Kover Gradle plugin in the `shared` module.

Running a single test (example):
- Run a single test class or method using Gradle's `--tests` filter on the target test task. Example (JVM):
  - `./gradlew :shared:jvmTest --tests "com.example.MyTestClass"`
  - For Android host tests: `./gradlew :shared:testAndroidHostTest --tests "com.example.MyTestClass"`

Lint / checks:
- General checks: `./gradlew check`
- Android lint: `./gradlew :androidApp:lintDebug` (or `:androidApp:lint` / `lintRelease`).

---

## High-level architecture
- This is a Kotlin Multiplatform (KMP) Compose project using Compose Multiplatform:
  - Modules at repo root: `shared` (KMP library), `androidApp`, `desktopApp`, and `iosApp` (Xcode entry).
  - `shared` provides `commonMain` plus platform-specific source sets (androidMain, jvmMain, iosMain, ...).
  - UI is shared via Compose Multiplatform; platform-specific entry points live in each app module.
  - iOS targets produce a static `Shared` framework (configured in `shared`'s build script).
- Build is Kotlin DSL Gradle (build.gradle.kts) and uses a version catalog at `gradle/libs.versions.toml` (plugins and deps referenced via `libs`/`alias`).
- Typical dependency pattern: app modules depend on `projects.shared` (e.g., `implementation(projects.shared)`).

---

## Key conventions and repo-specific patterns
- Gradle Kotlin DSL + version catalog:
  - Plugins and versions are referenced through `libs` (see `gradle/libs.versions.toml`). Build scripts use `alias(libs.plugins.xxx)`.
- Source set naming: prefer KMP canonical names (commonMain, androidMain, jvmMain, iosMain, ...).
- Tests are target-scoped: run the appropriate test task (e.g., `:shared:jvmTest`, `:shared:iosSimulatorArm64Test`). Use `--tests` to filter.
- Desktop hot reload: `:desktopApp:hotRun --auto` is the supported hot-reload flow for rapid UI iteration.
- iOS integration: `shared` configures iOS frameworks (static) and the `iosApp` stays the Xcode entry point — do not replace the Xcode project when adding iOS code.
- Android specifics:
  - Namespace is declared in `android` block (e.g., `de.geosphere.congregationplaner`).
  - Packaging excludes `META-INF` license files are set in `packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }`.
- Architecture guideline: Dieses Projekt soll gemäß dem Google "Leitfaden zur App-Architektur" aufgebaut werden (Schichten/Trennung: UI ⇄ ViewModel ⇄ Domain/Data, Single Source of Truth, UseCases/Repositories in shared where applicable). Favorisiere MVVM‑ähnliche Patterns mit ViewModels im shared‑Modul für gemeinsame Logik.
- Dependency Injection: Verwende Metro (dev.zacsweers.metro) als DI‑Framework. Lege Metro-Dependency-Graphs im `shared` Modul an und registriere platform‑spezifische BindingContainer und platform-spezifische Implementierungen in den jeweiligen sourceSets. Nutze @DependencyGraph, @Inject, @ContributesBinding/@ContributesTo und @Provides für Bindings. Füge das Metro Gradle‑Plugin in `shared/build.gradle.kts` hinzu: `id("dev.zacsweers.metro") version "1.2.1"`. Das Plugin fügt das multiplatform runtime‑Artefakt automatisch hinzu und konfiguriert den Compiler‑Plugin‑Wiring.
- Testing libraries: Für Coroutine/Flow‑Tests nutze Turbine (in `commonTest`). Unit‑Tests verwenden `kotlin.test`; benutze Turbine zum einfachen Testen von Flows und Stream‑Verhalten. Zum Mocken in Tests verwende MockK (`io.mockk:mockk`) und füge es als Test‑Dependency in den jeweiligen SourceSets hinzu (z. B. `testImplementation("io.mockk:mockk:<version>")`).
- Adding platforms or targets: follow the pattern in `shared/build.gradle.kts` — declare the target and configure binaries/source sets similarly.

---

## Where to look first (quick pointers for Copilot)
- Root README.md — run/dev commands and overview.
- `shared/build.gradle.kts` — KMP target declarations and dependencies.
- `androidApp/build.gradle.kts` — Android-specific config and appId/namespace.
- `gradle/libs.versions.toml` — authoritative versions and plugin aliases.

---

## Assistant/config artifacts
- No project-specific AI assistant config files (CLAUDE.md, AGENTS.md, .cursorrules, etc.) detected in the repo root.

---

Keep suggestions focused on using the Gradle wrapper, target-specific tasks, and Compose Multiplatform patterns above. When proposing changes that touch build scripts, prefer minimal edits that follow the existing `libs`/version-catalog and source-set structure.

---

Repository/DB Integration — Interface & UseCase Pattern (Konventionen)

Zweck:
Wenn eine Repository‑ oder Datenbankanbindung erforderlich ist, sind Interfaces (Repository/DataSource) und eine UseCase‑Schicht verpflichtend. Das trennt Domäne und Infrastruktur, verbessert Testbarkeit und macht DI explizit.

Schichten & Dateistruktur (empfohlen):
- domain: Interfaces (Repository), Domain‑Modelle, UseCases
  - Pfadbeispiel: shared/src/commonMain/kotlin/de/geosphere/congregationplaner/domain/
- data: Implementierungen, Mappers, DataSources (DB/Network)
  - Pfadbeispiel: shared/src/commonMain/kotlin/de/geosphere/congregationplaner/data/
- di: Metro‑Bindings im shared‑Module (DI‑Graph)
- presentation: ViewModels, State, Konsumenten der UseCases

Benennung & Signaturen:
- Repository Interface: suffix ‚Repository‘, z. B. UserRepository.
- Bevorzuge suspend‑Funktionen für einzelne Abfragen und Flow für Streams:
  - suspend fun getUser(id: String): Result<User>
  - fun observeUsers(): Flow<List<User>>
- UseCase: Klassen im domain/usecase‑Package, operator fun invoke(...) für einfachen Aufruf.

Implementierung & Mapping:
- Data‑Layer enthält konkrete DataSources (Room, SQLDelight, Retrofit).
- DTO ↔ Domain Mapping zentral in data/mappers.
- Platform‑konkrete Implementierungen können in platform‑sourceSets liegen und per DI gebunden werden.

Dependency Injection (Metro):
- Registriere Interface → Implementation Bindings in shared DI‑Graph.
- Platform‑spezifische DataSources in platform sourceSets bereitstellen und in Graph injizieren.

Tests:
- UseCases mit Fake/Mock Repository unit‑testen (keine Platformabhängigkeit).
- Mapper/Repository Integrationstests in passenden Gradle‑Targets (z. B. :shared:jvmTest oder :shared:testAndroidHostTest).

Fehlerbehandlung & Rückgabekonventionen:
- Standardisiere auf ein Rückgabe‑Pattern (z. B. kotlin.Result, Either, oder Resource/NetworkResult). Ein Projektweit einheitliches Pattern dokumentieren.

Checklist beim Hinzufügen eines Repositories:
1. Interface im domain‑Package anlegen.
2. UseCases definieren (domain/usecase).
3. Implementierung und Mappers in data hinzufügen.
4. DI‑Bindings in Metro konfigurieren.
5. Unit‑Tests für UseCases/Mapper schreiben.
6. Optional: Integrations‑Tests für DB/Network in platform‑Zielen.

Hinweis:
Diese Konventionen folgen den bestehenden KMP‑ und Metro‑Patterns im Projekt. Bei Abweichungen kurz kommentieren und begründen.

Beispiel Dateistruktur (ASCII-Baum) — KMP‑orientiert:

repo-root/
├─ README.md                         # Projektübersicht, Run/Build Hinweise
├─ settings.gradle.kts               # Gradle Settings (Module)
├─ gradle/
│  └─ wrapper/
├─ gradle.properties
├─ gradle/libs.versions.toml         # Version Catalog
├─ shared/                           # KMP shared module (Kotlin Multiplatform)
│  ├─ build.gradle.kts               # shared module build logic (targets, dependencies)
│  ├─ src/
│  │  ├─ commonMain/
│  │  │  ├─ kotlin/
│  │  │  │  └─ de/geosphere/congregationplaner/│  │  │  │     ├─ domain/
│  │  │  │     │  ├─ model/            # Domain‑Modelle (PLATFORM‑UNABHÄNGIG)
│  │  │  │     │  ├─ repository/       # Interfaces (UserRepository.kt)
│  │  │  │     │  └─ usecase/          # UseCases (GetUserUseCase.kt)
│  │  │  │     ├─ data/                # DTOs, shared mappers (platform‑agnostic impls)
│  │  │  │     │  ├─ repository/       # Impl stubs or multiplatform impls
│  │  │  │     │  └─ mappers/
│  │  │  │     └─ di/                  # Metro DI‑Graph config (shared bindings)
│  │  │  └─ resources/
│  │  ├─ commonTest/                  # Unit tests for domain and UseCases (kotlin.test)
│  │  ├─ androidMain/                  # Android‑konkrete Implementierungen (Room, Android APIs)
│  │  ├─ androidTest/
│  │  ├─ jvmMain/                      # Desktop/JVM specific code (if needed)
│  │  ├─ jvmTest/
│  │  ├─ iosMain/                      # iOS specific adapters (bridging to Shared framework)
│  │  ├─ iosSimulatorArm64Main/
│  │  └─ iosArm64Main/
│  └─ sql/                            # Optional: SQLDelight .sq files or migrations (if used)
├─ androidApp/                        # Android application module
│  ├─ build.gradle.kts
│  └─ src/main/
│     ├─ AndroidManifest.xml
│     └─ java|kotlin/                 # Android entrypoint, DI platform bindings, DB (Room) setup
├─ desktopApp/                        # Desktop entry (JVM) using Compose Multiplatform
│  └─ src/main/
├─ iosApp/                            # Xcode project — uses shared framework produced by shared module
│  └─ iosApp.xcodeproj
└─ .github/
   ├─ COPILOT_INSTRUCTIONS.md         # Diese Datei — Copilot Instruktionen (standard location)
   └─ workflows/                      # GitHub Actions if present

Erklärung & Hinweise:
- shared/src/commonMain: Nur plattformunabhängige Domänenlogik, Interfaces und UseCases. Keine Android/iOS APIs.
- shared/src/*Main (platform sourceSets): Platform‑konkrete DataSources/Adapters leben hier (Room adapters, SQLDelight drivers, HTTP clients if platform specific).
- DI: Metro shared bindings in shared/src/commonMain/kotlin/.../di; platform bindings in shared/src/androidMain/.../di or in androidApp module when required.
- DB: SQLDelight common .sq files can live in shared/sql; drivers/config in platform sourceSets. Room implementations live in androidMain.
- Tests: commonTest for UseCases/Domain; platform tests (androidTest/jvmTest/iosTest) for integration and DB tests.

Kurzanleitung (erweiterte Steps beim Hinzufügen eines Repositories):
1. Domain: Add interface in shared/src/commonMain/kotlin/.../domain/repository/YourRepo.kt.
2. UseCases: Add in domain/usecase/ and unit test in commonTest.
3. Data: Implement repository in shared/src/commonMain/data if multiplatform possible or in platform sourceSet (androidMain) for Room/Platform DB; add mappers in data/mappers.
4. SQL: Put SQLDelight schema in shared/sql when using SQLDelight; implement driver in platform sourceSets.
5. DI: Provide ContributesBinding/Provides in shared (common) and platform modules for platform implementations.
6. Tests: Unit tests in commonTest; integration DB tests in platform test tasks.

Dieses Baum‑Layout macht es für Copilot leichter, korrekte Pfade vorzuschlagen und Änderungen konsistent vorzunehmen.
