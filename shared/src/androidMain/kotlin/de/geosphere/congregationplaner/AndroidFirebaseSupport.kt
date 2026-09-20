package de.geosphere.congregationplaner

import android.content.Context
import com.google.firebase.FirebaseApp

actual class FirebasePlatformSupport {
    actual fun initialize() {
        val context = FirebaseAndroidContextHolder.context ?: return
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
    }

    actual fun isReady(): Boolean {
        val context = FirebaseAndroidContextHolder.context ?: return false
        return FirebaseApp.getApps(context).isNotEmpty()
    }
}

object FirebaseAndroidContextHolder {
    var context: Context? = null

    var useAuthEmulator: Boolean = false
        private set

    var authEmulatorHost: String = "10.0.2.2"
        private set

    fun configure(
        context: Context,
        useAuthEmulator: Boolean,
        authEmulatorHost: String = "10.0.2.2",
    ) {
        this.context = context
        this.useAuthEmulator = useAuthEmulator
        this.authEmulatorHost = authEmulatorHost
    }
}
