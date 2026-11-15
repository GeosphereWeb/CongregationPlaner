package de.geosphere.congregationplaner.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import de.geosphere.congregationplaner.MainActivity
import de.geosphere.congregationplaner.R
import de.geosphere.congregationplaner.notification.domain.model.NotificationCategory
import de.geosphere.congregationplaner.notification.domain.model.ReceivedNotification

/**
 * Zeigt eine Benachrichtigung an: in der Statuszeile das einfarbige `ic_stat_congregation`, in der Liste zusätzlich
 * das App-Icon. Je Kategorie gibt es einen Kanal, den der Nutzer in den Android-Einstellungen stummschalten kann.
 */
internal object NotificationPresenter {
    // Eine einzige Erinnerung „Redner informieren“, die sich täglich ersetzt und verschwindet, sobald alles erledigt ist.
    private const val SPEAKER_REMINDER_TAG = "speaker-reminder"

    fun show(context: Context, notification: ReceivedNotification) {
        // Die Dokument-ID als Tag: Wird dieselbe Benachrichtigung doppelt abgefragt, ersetzt sie sich selbst.
        post(
            context,
            notification.id,
            notification.category,
            notification.title,
            notification.body,
            notification.createdAt.toEpochMilliseconds(),
        )
    }

    fun showSpeakerReminder(context: Context, title: String, body: String) {
        post(context, SPEAKER_REMINDER_TAG, NotificationCategory.TALK_PLANNING, title, body, System.currentTimeMillis())
    }

    fun cancelSpeakerReminder(context: Context) {
        NotificationManagerCompat.from(context).cancel(SPEAKER_REMINDER_TAG, 0)
    }

    @Suppress("LongParameterList") // alles Bestandteile genau einer Benachrichtigung
    private fun post(
        context: Context,
        tag: String,
        category: NotificationCategory,
        title: String,
        body: String,
        whenMillis: Long,
    ) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!allowed) return
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(category.name, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(category.label)
                .build(),
        )
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val built = NotificationCompat.Builder(context, category.name)
            .setSmallIcon(R.drawable.ic_stat_congregation)
            // Das App-Icon ist ein Adaptive Icon; BitmapFactory könnte es nicht lesen.
            .setLargeIcon(ContextCompat.getDrawable(context, R.mipmap.ic_launcher)?.toBitmap())
            .setColor(ContextCompat.getColor(context, R.color.cp_primary))
            .setContentTitle(title)
            .setContentText(body)
            // Mehrere offene Termine stehen je in einer Zeile; die Kurzansicht zeigt nur die erste.
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setWhen(whenMillis)
            .setShowWhen(true)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        manager.notify(tag, 0, built)
    }
}
