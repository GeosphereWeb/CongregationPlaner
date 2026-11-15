package de.geosphere.congregationplaner.notification

import android.content.Context
import android.content.SharedPreferences
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.geosphere.congregationplaner.notification.di.NotificationGraph
import de.geosphere.congregationplaner.notification.di.createNotificationGraph
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Fragt im Hintergrund nach neuen Benachrichtigungen (ADR-0035) und prüft die Erinnerung „Redner informieren“
 * (CP-167). Der Stand beider Prüfungen liegt nur auf diesem Gerät.
 */
class NotificationCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val preferences = applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val graph = createNotificationGraph()
        // Beide Prüfungen laufen unabhängig; die nächste regelmäßige Abfrage versucht Fehlgeschlagenes wieder.
        val notified = checkNotifications(graph, preferences)
        val reminded = checkSpeakerReminder(graph, preferences)
        return if (notified && reminded) Result.success() else Result.failure()
    }

    private suspend fun checkNotifications(graph: NotificationGraph, preferences: SharedPreferences): Boolean {
        val seenUntil = preferences.getString(KEY_SEEN_UNTIL, null)?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val check = graph.checkNewNotifications(seenUntil, Clock.System.now()).getOrNull() ?: return false
        check.notifications.forEach { NotificationPresenter.show(applicationContext, it) }
        preferences.edit().putString(KEY_SEEN_UNTIL, check.seenUntil.toString()).apply()
        return true
    }

    private suspend fun checkSpeakerReminder(graph: NotificationGraph, preferences: SharedPreferences): Boolean {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val remindedOn = preferences.getString(KEY_REMINDED_ON, null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val check = graph.checkSpeakerReminders(now, remindedOn).getOrNull() ?: return false
        when {
            check.open.isEmpty() -> NotificationPresenter.cancelSpeakerReminder(applicationContext)
            check.remindNow -> {
                NotificationPresenter.showSpeakerReminder(applicationContext, check.title, check.body)
                preferences.edit().putString(KEY_REMINDED_ON, now.date.toString()).apply()
            }
        }
        return true
    }

    companion object {
        private const val PREFERENCES_NAME = "notifications"
        private const val KEY_SEEN_UNTIL = "seen_until"
        private const val KEY_REMINDED_ON = "speaker_reminded_on"
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
