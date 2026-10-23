# Firebase Auth und Firestore auf einem echten Android-Gerät

## Erkenntnis

Die Registrierung funktioniert im lokalen Android-Emulator, schlägt auf einem
per ADB verbundenen echten Android-Gerät jedoch mit folgender Meldung fehl:

```text
Failed to connect to /127.0.0.1:9099
```

Der Debug-Build aktiviert in `MainActivity` den Firebase Auth Emulator für jeden
debuggbaren Build. Die Host-Adresse wird aus dem Debug-Manifest gelesen:

```xml
<meta-data
    android:name="firebase_auth_emulator_host"
    android:value="127.0.0.1" />
```

`127.0.0.1` bezeichnet immer das Gerät, auf dem die Verbindung aufgebaut wird.
Auf dem echten Android-Gerät zeigt die Adresse deshalb auf das Gerät selbst und
nicht auf den Entwicklungsrechner, auf dem der Firebase Auth Emulator auf Port
`9099` und der Firestore Emulator auf Port `8080` laufen.

Der Firestore Emulator verwendet denselben Host wie der Auth Emulator
(`firebase_auth_emulator_host`). Ohne Weiterleitung von Port `8080` gelingt zwar
unter Umständen die Anmeldung, aber Firestore-Zugriffe (Mitglieder,
Versammlungen, Organisation) schlagen auf dem Gerät fehl.

Die Diagnose wurde im Android-Debugger auf dem Gerät `samsung SM-S928B`
bestätigt. Der Log zeigt außerdem:

```text
using auth emulator true at 127.0.0.1:9099
```

## Lösung für ein per ADB verbundenes Gerät

Die einfachste Lösung ist eine ADB-Portweiterleitung:

```text
adb reverse tcp:9099 tcp:9099
adb reverse tcp:8080 tcp:8080
```

Dadurch werden die Ports `9099` (Auth) und `8080` (Firestore) des Geräts auf die
gleichen Ports des Entwicklungsrechners weitergeleitet. Die bestehende
Konfiguration mit `127.0.0.1` kann dann unverändert bleiben.

Sind mehrere Geräte verbunden (zum Beispiel ein echtes Gerät und ein
Android-Emulator), muss das Ziel angegeben werden:

```text
adb -s <Seriennummer> reverse tcp:9099 tcp:9099
adb -s <Seriennummer> reverse tcp:8080 tcp:8080
```

Die Seriennummern zeigt `adb devices`.

Die Weiterleitung muss nach einer neuen ADB-Verbindung oder gegebenenfalls nach
einem Neustart des Geräts erneut gesetzt werden. Mit folgendem Befehl kann sie
kontrolliert werden:

```text
adb reverse --list
```

Die Ausgabe muss Einträge für `tcp:9099` und `tcp:8080` enthalten.

## Alternative Lösung über das lokale Netzwerk

Statt `127.0.0.1` kann die lokale IP-Adresse des Entwicklungsrechners verwendet
werden, zum Beispiel:

```xml
<meta-data
    android:name="firebase_auth_emulator_host"
    android:value="192.168.x.x" />
```

Dafür müssen folgende Voraussetzungen erfüllt sein:

- Das Android-Gerät und der Entwicklungsrechner befinden sich im selben
  Netzwerk.
- Der Firebase Emulator (Auth und Firestore) lauscht auf einer von außen
  erreichbaren Adresse, in `firebase.json` zum Beispiel `"host": "0.0.0.0"`.
- Die Windows-Firewall erlaubt eingehende Verbindungen auf die Ports `9099` und
  `8080`.
- Die IP-Adresse ist im Debug-Manifest korrekt eingetragen.

## Plattformabhängige Adressen

| Ausführungsumgebung | Host-Adresse |
| --- | --- |
| Android-Emulator | `10.0.2.2` |
| Echtes Gerät mit `adb reverse` | `127.0.0.1` |
| Echtes Gerät über WLAN/LAN | IP-Adresse des Entwicklungsrechners |
| Release-Build | Kein Auth Emulator, direkt Firebase |

Die Emulator-Nutzung sollte weiterhin auf Debug-Builds beschränkt bleiben.
Release-Builds müssen direkt gegen Firebase Authentication und Firestore laufen.

## Typische Fehler

| Symptom | Ursache |
| --- | --- |
| `Failed to connect to /127.0.0.1:9099` | `adb reverse tcp:9099 tcp:9099` fehlt oder der Auth Emulator läuft nicht. |
| Anmeldung klappt, Listen bleiben leer oder Firestore meldet Fehler | `adb reverse tcp:8080 tcp:8080` fehlt oder der Firestore Emulator läuft nicht. |
| `The password is invalid or the user does not have a password` | Der Emulator ist erreichbar, kennt die Kombination aber nicht (`INVALID_LOGIN_CREDENTIALS`). Das Konto unter `http://localhost:4000/auth` prüfen. Die Meldung erscheint auch, wenn das Konto nicht existiert. |
