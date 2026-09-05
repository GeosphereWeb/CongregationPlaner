package de.geosphere.congregationplaner

import cocoapods.FirebaseCore.FIRApp

actual class FirebasePlatformSupport() {
    private var initialized: Boolean = false

    actual fun initialize() {
        try {
            if (FIRApp.defaultApp() == null) {
                FIRApp.configure()
            }
            initialized = FIRApp.defaultApp() != null
        } catch (t: Throwable) {
            // iOS Firebase SDK nicht verfügbar oder Fehler bei der Initialisierung
            initialized = false
        }
    }

    actual fun isReady(): Boolean {
        return FIRApp.defaultApp() != null || initialized
    }
}
