package de.geosphere.congregationplaner.notification.testing

import de.geosphere.congregationplaner.notification.domain.model.AppNotification
import de.geosphere.congregationplaner.notification.domain.repository.NotificationRepository
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef

/** Merkt sich die gesendeten Benachrichtigungen mit Absender; [result] steuert Erfolg oder Fehler. */
internal class FakeNotificationRepository(var result: Result<Unit> = Result.success(Unit)) : NotificationRepository {
    val sent = mutableListOf<Pair<String, AppNotification>>()

    override suspend fun send(
        congregation: CongregationRef,
        senderUid: String,
        notification: AppNotification,
    ): Result<Unit> = result.onSuccess { sent += senderUid to notification }
}
