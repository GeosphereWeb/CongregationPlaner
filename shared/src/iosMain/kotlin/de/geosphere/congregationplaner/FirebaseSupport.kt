package de.geosphere.congregationplaner

// Platform-agnostic stub for iOS builds on non-mac hosts.
// Replace with real cocoapods FirebaseCore usage when developing on macOS.
actual class FirebasePlatformSupport() {
    private var initialized: Boolean = false

    actual fun initialize() {
        // No-op stub: iOS Firebase initialization is performed on macOS using CocoaPods (FIRApp.configure()).
        initialized = false
    }

    actual fun isReady(): Boolean {
        return initialized
    }
}
