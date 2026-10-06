package de.geosphere.congregationplaner.auth

actual class FirebasePlatformSupport {
    actual fun initialize() = initializeDesktopFirebase(
        projectId = DesktopEnv.getValue("FIREBASE_PROJECT_ID", "firebase.projectId"),
        apiKey = DesktopEnv.getValue("FIREBASE_WEB_API_KEY", "firebase.webApiKey"),
        emulatorHost = DesktopEnv.getValue("FIREBASE_AUTH_EMULATOR_HOST"),
        logError = System.err::println,
    )

    actual fun isReady(): Boolean = isDesktopFirebaseReady(
        DesktopEnv.getValue("FIREBASE_WEB_API_KEY", "firebase.webApiKey"),
    )
}

internal fun isDesktopFirebaseReady(apiKey: String?): Boolean = !apiKey.isNullOrBlank()

internal fun initializeDesktopFirebase(
    projectId: String?,
    apiKey: String?,
    emulatorHost: String?,
    logError: (String) -> Unit,
) {
    if (projectId.isNullOrBlank() && apiKey.isNullOrBlank()) {
        logError(
            "Firebase Desktop config missing. Please set FIREBASE_WEB_API_KEY and FIREBASE_PROJECT_ID. " +
                "Auth uses the local emulator at ${emulatorHost ?: "127.0.0.1:9099"}.",
        )
    }
}
