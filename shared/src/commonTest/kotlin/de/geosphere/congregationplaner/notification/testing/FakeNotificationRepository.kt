package de.geosphere.congregationplaner.notification.testing

import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.notification.domain.model.AppNotification
import de.geosphere.congregationplaner.notification.domain.model.ReceivedNotification
import de.geosphere.congregationplaner.notification.domain.repository.NotificationRepository
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import kotlin.time.Instant

/**
 * Merkt sich die gesendeten Benachrichtigungen mit Absender und die Abfragen; [result] steuert Erfolg oder Fehler
 * beim Senden, [fetchResult] die Antwort auf Abfragen.
 */
internal class FakeNotificationRepository(var result: Result<Unit> = Result.success(Unit)) : NotificationRepository {
    val sent = mutableListOf<Pair<String, AppNotification>>()
    var fetchResult: Result<List<ReceivedNotification>> = Result.success(emptyList())
    val fetches = mutableListOf<Triple<Set<EventAudience>, Set<CongregationRole>, Instant>>()

    override suspend fun send(
        congregation: CongregationRef,
        senderUid: String,
        notification: AppNotification,
    ): Result<Unit> = result.onSuccess { sent += senderUid to notification }

    override suspend fun fetchSince(
        congregation: CongregationRef,
        groups: Set<EventAudience>,
        roles: Set<CongregationRole>,
        since: Instant,
    ): Result<List<ReceivedNotification>> {
        fetches += Triple(groups, roles, since)
        return fetchResult
    }
}
