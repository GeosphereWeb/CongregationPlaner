# CongregationPlaner

[![CI](https://github.com/GeosphereWeb/CongregationPlaner/actions/workflows/ci.yml/badge.svg)](https://github.com/GeosphereWeb/CongregationPlaner/actions/workflows/ci.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=GeosphereWeb_CongregationPlaner&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=GeosphereWeb_CongregationPlaner)

CongregationPlaner ist eine **Kotlin Multiplatform**-App mit **Compose Multiplatform** zur Planung und Verwaltung
von Versammlungen. Die App läuft auf **Android**, **iOS** und **Desktop (JVM)** und nutzt **Firebase
Authentication** sowie **Cloud Firestore** als Backend.

## ✨ Funktionen

- **Anmeldung & Registrierung** über Firebase Authentication (inkl. Auth-Emulator für Debug-Builds)
- **Freigabe-Gate**: Neue Nutzer werden mit `approved = false` angelegt und müssen erst freigeschaltet werden
- **Rollenbasierte Berechtigungen** (`ADMIN`, `SPEAKING_PLANER`, `SPEAKING_ASSISTANT`, `NONE` – *deny by default*)
- **Versammlungs-Metadaten**: Versammlungen anlegen, bearbeiten und löschen (Name, Wochentag und Uhrzeit
  der Zusammenkünfte unter der Woche und am Wochenende) – Schreibrechte nur für freigegebene Admins
- Gemeinsame UI für alle Plattformen mit eigenem Theme (Light/Dark) und wiederverwendbarer Komponenten-Bibliothek

> **Geplant:** Planung unter der Woche, Planung Wochenende, Dienste und Nutzerverwaltung
> (bereits als Navigationspunkte angelegt).

## 🧱 Tech-Stack

| Bereich           | Technologie                                                        |
|-------------------|--------------------------------------------------------------------|
| Sprache / Build   | Kotlin 2.4, Gradle Kotlin DSL, Version Catalog (`gradle/libs.versions.toml`) |
| UI                | Compose Multiplatform, Material 3                                   |
| Architektur       | MVVM nach Googles „Leitfaden zur App-Architektur“ (UI ⇄ ViewModel ⇄ UseCase ⇄ Repository) |
| DI                | [Metro](https://github.com/ZacSweers/metro)                         |
| Async             | Kotlin Coroutines & Flow                                            |
| Backend           | Firebase Auth, Cloud Firestore (Android: SDK, Desktop: REST-API)    |
| Tests             | kotlin-test, Kotest Assertions, MockK, Turbine                      |
| Qualität          | Kover, Detekt, ktlint, SonarCloud, CodeQL                           |

## 📁 Projektstruktur

```
CongregationPlaner/
├── androidApp/     # Android-Einstiegspunkt (MainActivity)
├── desktopApp/     # Desktop-Einstiegspunkt (main.kt)
├── iosApp/         # Xcode-Projekt, bindet das Shared-Framework ein
├── shared/         # Geteilte Logik + UI (KMP)
│   └── src/
│       ├── commonMain/   # Plattformunabhängiger Code
│       │   ├── auth/           # Firebase-Auth-Abstraktion
│       │   ├── congregation/   # Feature: Versammlungen (data / di / domain / presentation)
│       │   └── user/           # Feature: Nutzerzugriff & Berechtigungen
│       ├── androidMain/  # Firebase Android SDK (Auth, Firestore)
│       ├── jvmMain/      # Desktop: Firebase REST, .env-Konfiguration
│       └── iosMain/      # iOS-spezifische Implementierungen
├── theming/        # AppTheme, AppColors, AppDimensions, Gradients
├── uilib/          # Wiederverwendbare Compose-Komponenten (Buttons, Cards, Dialogs, …)
├── config/detekt/  # Detekt-Konfiguration
└── gradle/         # Version Catalog & Kover-Konfiguration
```

Jedes Feature folgt dem Schichtenaufbau `data` → `domain` (`model`, `repository`, `usecase`, `permission`)
→ `presentation` (Screens & ViewModels) mit einem Metro-Graph unter `di`.

Weiterführende Doku:

- [PROJEKT_INFO.md](PROJEKT_INFO.md) – KMP-Grundlagen & Projektkonventionen
- [theming/README.md](theming/README.md) und [uilib/README.md](uilib/README.md) – Module im Detail
- [KOVER_SETUP.md](KOVER_SETUP.md) – Coverage-Konfiguration
- [FIREBASE_AUTH_DEVICE_DEBUGGING.md](FIREBASE_AUTH_DEVICE_DEBUGGING.md) – Auth-Emulator auf echten Geräten
- [.github/COPILOT_INSTRUCTIONS.md](.github/COPILOT_INSTRUCTIONS.md) – Architektur- & Coding-Richtlinien

## 🚀 Einrichtung

### Voraussetzungen

- JDK 17+
- Android Studio (aktuelle Version) mit Kotlin-Multiplatform-Plugin
- Für iOS: macOS mit Xcode

### Firebase-Konfiguration

Die Firebase-Konfigurationsdateien sind **nicht** im Repository enthalten und müssen lokal abgelegt werden:

| Plattform | Datei                                       |
|-----------|---------------------------------------------|
| Android   | `androidApp/google-services.json`           |
| iOS       | `iosApp/iosApp/GoogleService-Info.plist`    |
| Desktop   | `.env` im Projekt-Root                      |

Für Desktop wird die `.env` automatisch im aktuellen Verzeichnis und den übergeordneten Verzeichnissen gesucht;
Umgebungsvariablen funktionieren ebenfalls:

```dotenv
FIREBASE_WEB_API_KEY=...
FIREBASE_PROJECT_ID=...
# Optional: Auth-Emulator
FIREBASE_AUTH_EMULATOR_ENABLED=true
FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1
FIREBASE_AUTH_EMULATOR_PORT=9099
```

In der CI werden die Dateien aus den Secrets `GOOGLE_SERVICES_JSON_BASE64` und `IOS_GOOGLE_SERVICES_BASE64`
wiederhergestellt (Base64 erzeugen: `base64 -w 0 < datei`).

### Firestore-Datenmodell

| Collection                                                                   | Inhalt                 |
|------------------------------------------------------------------------------|------------------------|
| `users`                                                                      | Nutzer, Rolle, Freigabe |
| `region/EMEA/subregion/europe/branch/central-europe/circuit/609a/congregation` | Versammlungs-Metadaten |

> Die Berechtigungsprüfung in der App ersetzt **keine** Firestore Security Rules – diese müssen die Regeln
> serverseitig ebenfalls durchsetzen.

## ▶️ Apps starten

```bash
# Android (Debug-APK)
./gradlew :androidApp:assembleDebug

# Desktop
./gradlew :desktopApp:run
./gradlew :desktopApp:hotRun --auto   # mit Hot Reload
```

**iOS:** Das Verzeichnis [`iosApp`](iosApp) in Xcode öffnen und von dort starten.

Unter Windows statt `./gradlew` einfach `gradlew.bat` verwenden.

## 🧪 Tests & Qualität

```bash
./gradlew :shared:jvmTest                 # Desktop/JVM-Tests
./gradlew :shared:testAndroidHostTest     # Android-Host-Tests
./gradlew :shared:iosSimulatorArm64Test   # iOS-Simulator-Tests (macOS)
./gradlew :theming:jvmTest                # Theming-Tests

./gradlew :shared:jvmTest --tests "de.geosphere.congregationplaner.user.*"   # gefiltert

./gradlew check                           # alle Checks inkl. Detekt & ktlint
./gradlew koverMergedHtmlReport           # Coverage → build/reports/kover/merged/html/index.html
```

GitHub Actions bauen jeden Push und Pull Request (`ci.yml`) und führen zusätzlich SonarCloud- und CodeQL-Analysen aus.

## 🤝 Workflow & Konventionen

- **Branches:** `feature/CP-<nr>-<beschreibung>` für Tickets, `jdi/<beschreibung>` für kleine Änderungen
- **Commits:** `CP-<nr>: <gitmoji> <typ>: <beschreibung>`, z. B. `CP-9: ✨ feat: add, edit and delete congregations`
- **Dateiname = Klassenname**, keine Plattform-Suffixe wie `.android.kt`
- Abhängigkeiten und Versionen ausschließlich über `gradle/libs.versions.toml`
- Code in `commonMain` nutzt nur KMP-kompatible APIs (keine `java.*`- oder Android-Klassen)
