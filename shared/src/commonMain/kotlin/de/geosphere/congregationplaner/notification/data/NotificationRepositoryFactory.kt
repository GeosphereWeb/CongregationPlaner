package de.geosphere.congregationplaner.notification.data

import de.geosphere.congregationplaner.notification.domain.model.AppNotification
import de.geosphere.congregationplaner.notification.domain.repository.NotificationRepository
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef

expect fun createNotificationRepository(): NotificationRepository

internal const val NOTIFICATIONS_SUBCOLLECTION = "notifications"
internal const val CREATED_AT_FIELD = "createdAt"

internal val CongregationRef.notificationsPath: String get() = "$documentPath/$NOTIFICATIONS_SUBCOLLECTION"

/**
 * Das Feld `audience`: Gruppen und Rollen in einer Liste, damit Mitglieder mit einer einzigen
 * `array-contains-any`-Abfrage alles finden, was an sie gerichtet ist. Die Namen der Gruppen und Rollen überschneiden
 * sich nicht; die feste Reihenfolge lässt gleiche Benachrichtigungen gleich speichern.
 */
internal fun AppNotification.audienceValues(): List<String> =
    groups.sortedBy { it.ordinal }.map { it.name } + roles.sortedBy { it.ordinal }.map { it.name }

/** Die Felder, die die App schreibt; `createdAt` setzt jede Plattform als Serverzeit dazu. */
internal fun AppNotification.fieldValues(senderUid: String): Map<String, Any> = mapOf(
    "category" to category.name,
    "title" to title,
    "body" to body,
    "audience" to audienceValues(),
    "senderUid" to senderUid,
)

fun unavailableNotificationRepository(platform: String): NotificationRepository = object : NotificationRepository {
    override suspend fun send(
        congregation: CongregationRef,
        senderUid: String,
        notification: AppNotification,
    ): Result<Unit> = Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))
}
