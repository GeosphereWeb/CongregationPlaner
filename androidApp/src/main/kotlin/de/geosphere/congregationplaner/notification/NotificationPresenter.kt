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
import de.geosphere.congregationplaner.notification.domain.model.ReceivedNotification

/**
 * Zeigt eine Benachrichtigung an: in der Statuszeile das einfarbige `ic_stat_congregation`, in der Liste zusätzlich
 * das App-Icon. Je Kategorie gibt es einen Kanal, den der Nutzer in den Android-Einstellungen stummschalten kann.
 */
internal object NotificationPresenter {
    fun show(context: Context, notification: ReceivedNotification) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!allowed) return
        val manager = NotificationManagerCompat.from(context)
        val category = notification.category
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
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setWhen(notification.createdAt.toEpochMilliseconds())
            .setShowWhen(true)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        // Die Dokument-ID als Tag: Wird dieselbe Benachrichtigung doppelt abgefragt, ersetzt sie sich selbst.
        manager.notify(notification.id, 0, built)
    }
}
