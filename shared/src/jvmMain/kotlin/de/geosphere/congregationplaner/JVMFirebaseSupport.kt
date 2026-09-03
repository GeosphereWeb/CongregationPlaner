package de.geosphere.congregationplaner

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

actual class FirebasePlatformSupport {
    actual fun initialize() {
        val options = loadDesktopFirebaseOptions() ?: run {
            System.err.println(
                "Firebase Desktop config missing. Please set FIREBASE_PROJECT_ID (and optional FIREBASE_DATABASE_URL / FIREBASE_STORAGE_BUCKET).",
            )
            return
        }

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        }
    }

    actual fun isReady(): Boolean = FirebaseApp.getApps().isNotEmpty()
}

private fun loadDesktopFirebaseOptions(): FirebaseOptions? {
    val projectId = System.getenv("FIREBASE_PROJECT_ID") ?: System.getProperty("firebase.projectId")

    if (projectId.isNullOrBlank()) {
        return null
    }

    val builder = FirebaseOptions.builder()
        .setProjectId(projectId)

    val databaseUrl = System.getenv("FIREBASE_DATABASE_URL") ?: System.getProperty("firebase.databaseUrl")
    if (!databaseUrl.isNullOrBlank()) {
        builder.setDatabaseUrl(databaseUrl)
    }

    val storageBucket = System.getenv("FIREBASE_STORAGE_BUCKET") ?: System.getProperty("firebase.storageBucket")
    if (!storageBucket.isNullOrBlank()) {
        builder.setStorageBucket(storageBucket)
    }

    return builder.build()
}
