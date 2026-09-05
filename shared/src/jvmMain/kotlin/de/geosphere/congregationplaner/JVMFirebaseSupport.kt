package de.geosphere.congregationplaner

actual class FirebasePlatformSupport {
    actual fun initialize() {
        val projectId = DesktopEnvLoader.getValue("FIREBASE_PROJECT_ID", "firebase.projectId")
        val apiKey = DesktopEnvLoader.getValue("FIREBASE_WEB_API_KEY", "firebase.webApiKey")

        if (projectId.isNullOrBlank() && apiKey.isNullOrBlank()) {
            System.err.println(
                "Firebase Desktop config missing. Please set FIREBASE_WEB_API_KEY and FIREBASE_PROJECT_ID (optional: FIREBASE_DATABASE_URL / FIREBASE_STORAGE_BUCKET).",
            )
            return
        }

        // The desktop auth flow uses Firebase Identity Toolkit REST endpoints and the web API key.
        // Initializing FirebaseApp here is not required and would fail without service-account credentials.
    }

    actual fun isReady(): Boolean =
        !DesktopEnvLoader.getValue("FIREBASE_WEB_API_KEY", "firebase.webApiKey").isNullOrBlank()
}
