package de.geosphere.congregationplaner

actual class FirebasePlatformSupport() {
    actual fun initialize() {
        // TODO: Firebase für iOS initialisieren
    }

    actual fun isReady(): Boolean {
        // TODO: Prüfen, ob Firebase bereit ist
        return false
    }
}
