package de.geosphere.congregationplaner.auth

actual class FirebasePlatformSupport {
    actual fun initialize() {
        val projectId = DesktopEnv.getValue("FIREBASE_PROJECT_ID", "firebase.projectId")
        val apiKey = DesktopEnv.getValue("FIREBASE_WEB_API_KEY", "firebase.webApiKey")

        if (projectId.isNullOrBlank() && apiKey.isNullOrBlank()) {
            System.err.println(
                "Firebase Desktop config missing. Please set FIREBASE_WEB_API_KEY and FIREBASE_PROJECT_ID. " +
                    "Auth uses the local emulator at ${DesktopEnv.getValue(
                        "FIREBASE_AUTH_EMULATOR_HOST",
                    ) ?: "127.0.0.1:9099"}.",
            )
            return
        }

        // The desktop auth flow uses Firebase Identity Toolkit REST endpoints and the web API key.
        // Initializing FirebaseApp here is not required and would fail without service-account credentials.
    }

    actual fun isReady(): Boolean = isDesktopFirebaseReady(
        DesktopEnv.getValue("FIREBASE_WEB_API_KEY", "firebase.webApiKey"),
    )
}

internal fun isDesktopFirebaseReady(apiKey: String?): Boolean = !apiKey.isNullOrBlank()
