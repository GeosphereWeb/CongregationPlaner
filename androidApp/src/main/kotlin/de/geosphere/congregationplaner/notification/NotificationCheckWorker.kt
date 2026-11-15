package de.geosphere.congregationplaner.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.geosphere.congregationplaner.notification.di.createCheckNewNotificationsUseCase
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Fragt im Hintergrund nach neuen Benachrichtigungen und zeigt sie an (ADR-0035). Der Stand der letzten Abfrage liegt
 * nur auf diesem Gerät.
 */
class NotificationCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val preferences = applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val seenUntil = preferences.getString(KEY_SEEN_UNTIL, null)?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val check = createCheckNewNotificationsUseCase()(seenUntil, Clock.System.now()).getOrElse {
            // Die nächste regelmäßige Abfrage versucht es wieder; kein Wiederholen, um bei Fehlern nicht zu drängeln.
            return Result.failure()
        }
        check.notifications.forEach { NotificationPresenter.show(applicationContext, it) }
        preferences.edit().putString(KEY_SEEN_UNTIL, check.seenUntil.toString()).apply()
        return Result.success()
    }

    companion object {
        private const val PREFERENCES_NAME = "notifications"
        private const val KEY_SEEN_UNTIL = "seen_until"
        private const val PERIODIC_WORK = "notification-check"
        private const val IMMEDIATE_WORK = "notification-check-now"
        private const val INTERVAL_MINUTES = 15L // das Minimum von WorkManager

        private val onlineOnly = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Alle 15 Minuten, solange die App installiert ist; ein vorhandener Auftrag bleibt bestehen. */
        fun schedulePeriodic(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<NotificationCheckWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
                    .setConstraints(onlineOnly)
                    .build(),
            )
        }

        /** Sofort, z. B. beim Öffnen der App. */
        fun checkNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<NotificationCheckWorker>().setConstraints(onlineOnly).build(),
            )
        }
    }
}
