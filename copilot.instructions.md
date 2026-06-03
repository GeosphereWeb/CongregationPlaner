# Copilot instructions (alternate)

Kurze Hinweise für Copilot-bezogene Aktionen im Projekt.

- Coverage: Use Kover (Kotlin Kover) for test coverage reporting.
  - Add the Kover Gradle plugin to the `shared` module (plugin id: `org.jetbrains.kotlinx.kover`).
  - Run coverage report: `./gradlew koverReport` (Windows: `gradlew.bat koverReport`).
  - Configure the plugin in `shared/build.gradle.kts` as appropriate for project reporting and thresholds.

See the official Kotlin Kover documentation for examples and plugin versioning.
