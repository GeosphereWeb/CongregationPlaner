package de.geosphere.congregationplaner

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import de.geosphere.congregationplaner.auth.data.FirebaseAndroidContextHolder
import de.geosphere.congregationplaner.auth.data.FirebaseSupport
import de.geosphere.congregationplaner.notification.NotificationCheckWorker

/**
 * Konfiguriert Firebase für jeden Start des Prozesses, auch ohne Activity: Die Abfrage der Benachrichtigungen läuft im
 * Hintergrund und braucht dieselbe Datenbank (Emulator im Debug-Build) wie die Oberfläche (ADR-0035).
 */
class CongregationPlanerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val metaData = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData
        FirebaseAndroidContextHolder.configure(
            context = applicationContext,
            activity = null,
            useAuthEmulator = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
            authEmulatorHost = metaData
                ?.getString("firebase_auth_emulator_host")
                ?.takeIf { it.isNotBlank() }
                ?: "10.0.2.2",
        )
        FirebaseSupport.initialize()
        NotificationCheckWorker.schedulePeriodic(this)
    }
}
