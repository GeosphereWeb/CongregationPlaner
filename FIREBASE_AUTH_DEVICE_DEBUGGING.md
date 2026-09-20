# Firebase Auth auf einem echten Android-Gerät

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
`9099` läuft.

Die Diagnose wurde im Android-Debugger auf dem Gerät `samsung SM-S928B`
bestätigt. Der Log zeigt außerdem:

```text
using auth emulator true at 127.0.0.1:9099
```

## Lösung für ein per ADB verbundenes Gerät

Die einfachste Lösung ist eine ADB-Portweiterleitung:

```text
adb reverse tcp:9099 tcp:9099
```

Dadurch wird der Port `9099` des Geräts auf den Port `9099` des
Entwicklungsrechners weitergeleitet. Die bestehende Konfiguration mit
`127.0.0.1` kann dann unverändert bleiben.

Die Weiterleitung muss nach einer neuen ADB-Verbindung oder gegebenenfalls nach
einem Neustart des Geräts erneut gesetzt werden. Mit folgendem Befehl kann sie
kontrolliert werden:

```text
adb reverse --list
```

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
- Der Firebase Emulator lauscht auf einer von außen erreichbaren Adresse.
- Die Windows-Firewall erlaubt eingehende Verbindungen auf Port `9099`.
- Die IP-Adresse ist im Debug-Manifest korrekt eingetragen.

## Plattformabhängige Adressen

| Ausführungsumgebung | Host-Adresse |
| --- | --- |
| Android-Emulator | `10.0.2.2` |
| Echtes Gerät mit `adb reverse` | `127.0.0.1` |
| Echtes Gerät über WLAN/LAN | IP-Adresse des Entwicklungsrechners |
| Release-Build | Kein Auth Emulator, direkt Firebase |

Die Emulator-Nutzung sollte weiterhin auf Debug-Builds beschränkt bleiben.
Release-Builds müssen direkt gegen Firebase Authentication laufen.
