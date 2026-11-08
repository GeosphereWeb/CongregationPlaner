# CLAUDE.md

Kotlin Multiplatform + Compose Multiplatform Projekt (Congregation Planer). Diese Datei wird von Claude Code automatisch zu Beginn jeder Session geladen. Kurz halten; Details stehen im Code, in `README.md` und in `docs/adr/`.

## Module

- `shared` — KMP-Library: Domain, Data, DI, ViewModels/Presenter, gemeinsame UI (`commonMain` plus `androidMain`, `jvmMain`, `iosMain`)
- `androidApp`, `desktopApp` — Plattform-Einstiegspunkte, hängen von `projects.shared` ab
- `iosApp` — Xcode-Projekt (nicht ersetzen, nur `shared`-Framework einbinden)
- `uilib`, `theming` — wiederverwendbare UI-Bausteine und Design-Tokens

Namespace/Package: `de.geosphere.congregationplaner`. Fachpakete in `shared`: `auth`, `congregation`, `organization`, `user`, `circuit`, `presentation`.

## Befehle

Gradle Wrapper verwenden (`./gradlew`, unter Windows PowerShell `.\gradlew.bat`).

- Alles bauen: `./gradlew assemble`
- Android Debug-APK: `./gradlew :androidApp:assembleDebug`
- Desktop: `./gradlew :desktopApp:run` (Hot Reload: `:desktopApp:hotRun --auto`)
- Checks/Lint: `./gradlew check`, Android Lint: `./gradlew :androidApp:lintDebug`
- Tests (target-spezifisch): `:shared:jvmTest`, `:shared:testAndroidHostTest`, `:shared:iosSimulatorArm64Test`
- Einzelner Test: `./gradlew :shared:jvmTest --tests "de.geosphere.congregationplaner.MyTest"`
- Coverage: `./gradlew koverReport` (siehe `KOVER_SETUP.md`)
- Firestore-Rules: Alle Firebase-Dateien liegen in `firebase/`. Bei Änderungen an `firebase/firestore.rules` vor dem Commit den Rules-Test gegen den Emulator ausführen: `node firebase/rules-test/run.mjs` (siehe `firebase/rules-test/README.md`)

- Format/Lint: ktlint (`./gradlew ktlintCheck`, `ktlintFormat`) und detekt (`./gradlew detekt`, Regeln in `config/detekt/detekt.yml`); SonarCloud läuft in der CI

Nach Code-Änderungen mindestens den passenden `jvmTest` und `check` laufen lassen, bevor etwas als fertig gemeldet wird. Ist ein Lauf nicht möglich (z. B. iOS unter Windows), das explizit sagen.

## Codestil

- ktlint-Regeln stehen in `.editorconfig` (4 Leerzeichen, LF, Final-Newline, UTF-8). Neuen Code danach formatieren, aber nicht ganze bestehende Dateien neu formatieren.
- `./gradlew check` führt ktlint und detekt mit aus; Verstöße beheben statt unterdrücken. Neue Suppressions oder Baseline-Einträge nur mit Begründung.

## Architektur

Google-Leitfaden zur App-Architektur: UI ⇄ ViewModel ⇄ Domain ⇄ Data, Single Source of Truth, MVVM mit ViewModels in `shared`.

- `domain/` — Modelle, Repository-Interfaces, UseCases (`domain/usecase`, `operator fun invoke`)
- `data/` — Repository-Implementierungen, DataSources, zentrale DTO↔Domain-Mapper (`data/mappers`)
- `di/` — Metro-Bindings (`@DependencyGraph`, `@Inject`, `@ContributesBinding`, `@Provides`); plattformspezifische Bindings in den jeweiligen SourceSets
- `presentation/` — ViewModels, State, UseCase-Konsumenten

Bei Repository-/DB-Anbindung sind Interface + UseCase-Schicht Pflicht. Repository-Interfaces enden auf `Repository`; `suspend fun` für Einzelabfragen, `Flow` für Streams. Fehler einheitlich über ein projektweites Rückgabe-Pattern (z. B. `Result`) behandeln.

## KMP-Regeln

- `commonMain`: nur Kotlin/KMP-kompatible APIs. Kein `java.*` (z. B. `java.util.UUID`, `java.io.*`), keine Android-Klassen.
- Plattformcode in `androidMain`/`iosMain`/`jvmMain`; `expect`/`actual` nur, wenn ein Interface + DI nicht reicht.
- Async mit Coroutines/`Flow`. Neue Libraries müssen KMP-kompatibel sein und über `gradle/libs.versions.toml` laufen (`alias(libs.…)`), keine Versionen direkt in Build-Skripten (Ausnahme: das bestehende Metro-Plugin in `shared/build.gradle.kts`).
- Kein natives SwiftUI, außer ausdrücklich verlangt.
- Build-Änderungen minimal halten und der vorhandenen Struktur (Version Catalog, SourceSets) folgen.

## UI

- Jedes neue oder geänderte UI-Element (Composable) bekommt eine Preview-Funktion in derselben Datei (`private fun …Preview()`) mit `@ThemePreviews` (Light/Dark) und `PreviewThemeWrapper` bzw. `NightPreviewWrapper`, die die relevanten Zustände zeigt (z. B. an/aus, deaktiviert, Fehler). Composables, die direkt an ViewModel oder Navigation hängen, werden in einen zustandslosen Teil mit Preview aufgeteilt. Previews sind reine Darstellung und brauchen keinen eigenen Test.

## Tests

- `kotlin.test` als Basis, Turbine für Flows, MockK für Mocks (Versionen im Catalog).
- Tests sind Pflicht: Jede neue oder geänderte Logik (Code, Mapper, Repositories, Firestore-Regeln) wird im selben PR mit Tests bereitgestellt. Ein Bugfix bekommt einen Test, der ohne den Fix fehlschlägt. Reine Dokumentation und reine Darstellung ohne Logik sind die einzigen Ausnahmen; sie werden im PR begründet.
- Die Tests müssen in der Pipeline (GitHub Actions, `.github/workflows/sonarcloud.yml`: `:shared:jvmTest` und `:shared:testAndroidHostTest`) laufen und grün sein. Ein Test, den die Pipeline nicht ausführt (z. B. der Firestore-Rules-Test `firebase/rules-test`), zählt nicht als Absicherung: dann zusätzlich die Pipeline erweitern oder im PR ausdrücklich nennen, dass er nur lokal läuft. Tests werden nie entfernt, übersprungen oder abgeschwächt, um eine Pipeline grün zu bekommen. iOS-Tests sind vorerst nicht erforderlich, solange für iOS noch nicht aktiv entwickelt wird (gilt bis auf Widerruf).
- UseCases/Domain in `commonTest` mit Fakes oder Mocks testen; Mapper und Repository-Integration in passenden Plattform-Targets.

## Sprache

- Chat-Antworten und Dokumentation (README, ADRs, PR-Beschreibungen): Deutsch.
- Code, Bezeichner, Code-Kommentare und Commit-Texte: Englisch.

## Windows-Umgebung

- Entwicklung läuft unter Windows (PowerShell und Git Bash). iOS-Tasks (`iosSimulatorArm64Test`, Xcode) sind dort nicht ausführbar.
- Zeilenenden: LF ist überall Standard (`.editorconfig` `end_of_line = lf`; `.gitattributes` setzt `* text=auto eol=lf`, nur `gradlew.bat` bleibt CRLF). Neue und geänderte Dateien immer mit LF speichern; die Git-Warnung „LF will be replaced by CRLF“ sollte damit nicht mehr auftreten.

## Git, Commits und PRs

- Commit-Header und PR-Titel müssen mit `CP-<nr>:` oder `JDI:` beginnen, optional gefolgt von einem Emoji, z. B. `CP-14: 📝 docs: add ADR README` (Prüfung in `.github/workflows/mergeable.yml`).
- Jede Änderung (Feature, Fix, Doku, Regeln) erfolgt in einem eigenen Branch mit eigenem PR und eigenem Jira-Ticket (Projekt `CP`). Die Ticket-ID vergibt Jira: erst das Ticket anlegen, dann den Branch und die Commits mit dieser Nummer benennen; Nummern nie selbst erfinden. Gehören mehrere Änderungen in einem Dialog thematisch zusammen, dürfen sie im selben Ticket, Branch und PR bearbeitet werden; andere Themen bekommen ein eigenes Ticket.
- Branch-Schema: `feature/CP-<nr>-kurzbeschreibung` (kebab-case). Die Ticketnummer aus dem Branchnamen als Commit-Prefix verwenden.
- PRs nutzen `.github/pull_request_template.md`.
- Ein Branch wird für einen PR nur freigegeben, wenn er auf den aktuellen `master` gerebased ist (`git fetch` + `git rebase origin/master`, kein Merge von `master` in den Branch). Danach mit `git push --force-with-lease` pushen, nie mit `--force`.
- PRs bestehen aus wenigen Commits, die je ein Thema oder einen logischen Baustein abbilden (z. B. Fix, Tests, Doku); Korrektur-, Test-Nachzügler- und Review-Commits werden vor der Freigabe per Fixup/Squash in den passenden Commit gefaltet. Dabei die Commit-Regeln (`CP-<nr>:`-Header) einhalten und anschließend mit `--force-with-lease` pushen.
- Nur auf ausdrückliche Anfrage committen oder pushen. Nie direkt auf `master`.

## Testumgebung und Altdaten

Die App läuft bis zum Go-live nur in einer Testumgebung (gilt bis auf Widerruf). Code und Firestore-Regeln berücksichtigen keine Altdaten: keine Migration, keine Kompatibilitätslogik für alte Felder oder Formate, keine Rücksicht auf ältere deployte Regeln. Das Datenmodell wird direkt auf den Soll-Zustand geändert. Bereits vorhandene Altdaten-Behandlung wird entfernt, sobald sie auffällt.

## ADRs

Architekturrelevante Entscheidungen (neues Pattern, Modulgrenze, Datenfluss, nicht offensichtlicher Trade-off) als ADR in `docs/adr/` festhalten, im selben PR wie die Entscheidung. Format und Regeln: `docs/adr/README.md`. Nummern nie wiederverwenden.

## Hinweise zum Arbeiten

- `.env`, `local.properties` und Firebase-Konfiguration enthalten Geheimnisse: nicht lesen, ausgeben oder committen.
- Firebase-Skills liegen in `.agents/skills/`; bei Firebase-Aufgaben dort nachsehen.
- Weitere Hintergründe: `PROJEKT_INFO.md`, `HoTo.md`.
