package de.geosphere.congregationplaner.notification.data

import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.notification.domain.model.AppNotification
import de.geosphere.congregationplaner.notification.domain.model.NotificationCategory
import de.geosphere.congregationplaner.notification.domain.model.ReceivedNotification
import de.geosphere.congregationplaner.notification.domain.repository.NotificationRepository
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import kotlin.time.Instant

expect fun createNotificationRepository(): NotificationRepository

internal const val NOTIFICATIONS_SUBCOLLECTION = "notifications"
internal const val AUDIENCE_FIELD = "audience"
internal const val CREATED_AT_FIELD = "createdAt"

/** Höchstens so viele Benachrichtigungen je Abfrage; mehr zeigt das Gerät auf einmal ohnehin nicht sinnvoll an. */
internal const val FETCH_LIMIT = 20L

internal val CongregationRef.notificationsPath: String get() = "$documentPath/$NOTIFICATIONS_SUBCOLLECTION"

/**
 * Das Feld `audience`: Gruppen und Rollen in einer Liste, damit Mitglieder mit einer einzigen
 * `array-contains-any`-Abfrage alles finden, was an sie gerichtet ist. Die Namen der Gruppen und Rollen überschneiden
 * sich nicht; die feste Reihenfolge lässt gleiche Benachrichtigungen gleich speichern.
 */
internal fun audienceValues(groups: Set<EventAudience>, roles: Set<CongregationRole>): List<String> =
    groups.sortedBy { it.ordinal }.map { it.name } + roles.sortedBy { it.ordinal }.map { it.name }

/** Die Felder, die die App schreibt; `createdAt` setzt jede Plattform als Serverzeit dazu. */
internal fun AppNotification.fieldValues(senderUid: String): Map<String, Any> = mapOf(
    "category" to category.name,
    "title" to title,
    "body" to body,
    AUDIENCE_FIELD to audienceValues(groups, roles),
    "senderUid" to senderUid,
)

/**
 * Baut eine gelesene Benachrichtigung aus den Textfeldern, die [field] liefert; ohne bekannte Kategorie oder Zeit wird
 * sie übersprungen.
 */
internal fun receivedNotificationOf(
    id: String,
    createdAt: Instant?,
    field: (String) -> String?,
): ReceivedNotification? {
    val category = NotificationCategory.entries.firstOrNull { it.name == field("category") }
    if (category == null || createdAt == null) return null
    return ReceivedNotification(
        id = id,
        category = category,
        title = field("title").orEmpty(),
        body = field("body").orEmpty(),
        senderUid = field("senderUid").orEmpty(),
        createdAt = createdAt,
    )
}

fun unavailableNotificationRepository(platform: String): NotificationRepository = object : NotificationRepository {
    private fun <T> unavailable(): Result<T> =
        Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

    override suspend fun send(
        congregation: CongregationRef,
        senderUid: String,
        notification: AppNotification,
    ): Result<Unit> = unavailable()

    override suspend fun fetchSince(
        congregation: CongregationRef,
        groups: Set<EventAudience>,
        roles: Set<CongregationRole>,
        since: Instant,
    ): Result<List<ReceivedNotification>> = unavailable()
}
