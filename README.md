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
  (Kurzfassung im README-Abschnitt [Lokaler Firebase-Emulator](#-lokaler-firebase-emulator-inkl-echtes-android-gerät))
- [CLAUDE.md](CLAUDE.md) – Architektur- & Coding-Richtlinien

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

## 🔥 Lokaler Firebase-Emulator (inkl. echtes Android-Gerät)

Debug-Builds der Android-App sprechen nicht mit dem echten Firebase-Projekt, sondern mit dem lokalen
**Firebase Emulator** auf dem Entwicklungsrechner:

| Dienst         | Port   | Verwendung                              |
|----------------|--------|-----------------------------------------|
| Authentication | `9099` | Anmeldung & Registrierung               |
| Firestore      | `8080` | Mitglieder, Versammlungen, Organisation |
| Emulator-UI    | `4000` | Konten und Dokumente ansehen/bearbeiten |

Release-Builds nutzen immer direkt Firebase, der Emulator wird nur in debuggbaren Builds aktiviert.

### 1. Emulator starten

Voraussetzung: [Firebase CLI](https://firebase.google.com/docs/cli) (`npm install -g firebase-tools`) und ein
Ordner mit `firebase.json`, in dem Auth (`9099`) und Firestore (`8080`) konfiguriert sind. Dort:

```bash
firebase emulators:start --only auth,firestore --import ./emulator-data --export-on-exit
```

`--import` und `--export-on-exit` sorgen dafür, dass Auth-Konten **und** Firestore-Daten einen Neustart überleben.
Ohne diese Optionen sind nach jedem Neustart alle Konten und Daten weg, und der Login schlägt fehl.

### 2. Android-Emulator (AVD) oder physisches Gerät verbinden

Die Debug-App erwartet den Emulator unter `127.0.0.1` (siehe `androidApp/src/debug/AndroidManifest.xml`,
`firebase_auth_emulator_host`). Auf einem Android-Gerät bedeutet `127.0.0.1` aber **das Gerät selbst**, nicht den
Rechner. Deshalb müssen die Ports per ADB vom Gerät zum Rechner weitergeleitet werden:

1. **USB-Debugging aktivieren:** Entwickleroptionen freischalten (7× auf die *Build-Nummer* tippen), dann
   *Entwickleroptionen → USB-Debugging* einschalten.
2. **Gerät per USB anschließen** und die Debugging-Abfrage am Gerät bestätigen.
3. **Verbindung prüfen:** das Gerät muss als `device` erscheinen (nicht als `unauthorized`):

   ```bash
   adb devices
   ```

4. **Ports weiterleiten:** beide Befehle sind nötig, sonst klappt zwar der Login, aber nicht der Firestore-Zugriff:

   ```bash
   adb reverse tcp:9099 tcp:9099   # Auth
   adb reverse tcp:8080 tcp:8080   # Firestore
   ```

5. **Kontrollieren:**

   ```bash
   adb reverse --list
   ```

   Erwartet werden Einträge für `tcp:9099` und `tcp:8080`.
6. **Debug-App starten.** Im Logcat (Filter `FirebaseAuth`) steht dann
   `using auth emulator true at 127.0.0.1:9099`.

Wichtig dabei:

- `adb reverse` gilt **nur bis zum Trennen des Kabels, Neustart des Geräts oder von `adb`**. Danach beide Befehle
  erneut ausführen.
- Sind mehrere Geräte verbunden (Gerät **und** AVD), das Ziel mit `-s <Seriennummer>` angeben:
  `adb -s <Seriennummer> reverse tcp:9099 tcp:9099` (Seriennummern zeigt `adb devices`).
- Der AVD braucht mit `127.0.0.1` im Manifest dieselben `adb reverse`-Befehle, alternativ erreicht er den Rechner
  über `10.0.2.2`.

| Ausführungsumgebung            | Host im Manifest                      |
|--------------------------------|---------------------------------------|
| Android-Emulator (AVD)         | `10.0.2.2` (oder `adb reverse`)       |
| Echtes Gerät mit `adb reverse` | `127.0.0.1` (Standard im Repo)        |
| Echtes Gerät über WLAN/LAN     | IP-Adresse des Rechners (siehe unten) |

**Alternative ohne Kabel (WLAN):** `firebase_auth_emulator_host` im Debug-Manifest auf die IP des Rechners setzen,
im `firebase.json` bei `auth` und `firestore` `"host": "0.0.0.0"` eintragen, Gerät und Rechner ins selbe Netz
bringen und in der Windows-Firewall eingehende Verbindungen auf `9099` und `8080` erlauben. Die Emulatoren sind dann
im ganzen Netz erreichbar – nur in vertrauenswürdigen Netzen verwenden.
Weitere Hintergründe: [FIREBASE_AUTH_DEVICE_DEBUGGING.md](FIREBASE_AUTH_DEVICE_DEBUGGING.md).

### 3. Ersten Admin anlegen

Die App legt aus Sicherheitsgründen **keinen** Admin an. Neue Nutzer starten immer mit `role = MEMBER` und
`approved = false`, und Admins können ihre eigene Freigabe nicht ändern. Der erste Admin wird deshalb einmalig von
Hand eingetragen:

1. In der App registrieren und die Versammlung wählen. Dabei entsteht das Dokument
   `region/…/congregation/{id}/members/{uid}`.
2. In der Emulator-UI (`http://localhost:4000/firestore`) dieses Dokument öffnen und `approved` auf `true` sowie
   `role` auf `CONGREGATION_ADMIN` setzen.
3. In der App ab- und wieder anmelden. Ab jetzt können weitere Mitglieder in der Userverwaltung freigegeben werden.

Alternativ macht ein Dokument `admins/{uid}` (UID aus *Authentication* in der Emulator-UI) jemanden zum globalen Admin.

### Fehlersuche

| Symptom                                                                                         | Ursache / Lösung |
|-------------------------------------------------------------------------------------------------|------------------|
| `Failed to connect to /127.0.0.1:9099`                                                          | `adb reverse` fehlt oder ist verloren gegangen, oder der Emulator läuft nicht. |
| Login klappt, aber Listen bleiben leer / Firestore-Fehler                                       | `adb reverse tcp:8080 tcp:8080` fehlt oder der Firestore-Emulator läuft nicht. |
| „Login fehlgeschlagen“, Logcat: `The password is invalid or the user does not have a password`  | Der Emulator ist erreichbar, kennt die Kombination aber nicht (`INVALID_LOGIN_CREDENTIALS`). Konto unter `http://localhost:4000/auth` prüfen, neu registrieren oder Passwort dort neu setzen. Auch bei nicht existierendem Konto kommt diese Meldung. |
| Nutzer erscheint nicht in der Mitgliederliste                                                   | Das Mitgliedsdokument entsteht erst bei der ersten App-Anmeldung mit gewählter Versammlung. Ein nur in der Emulator-UI angelegtes Auth-Konto reicht nicht. |
| Konten oder Daten nach Neustart weg                                                             | Emulatoren ohne `--import` / `--export-on-exit` gestartet. |

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
