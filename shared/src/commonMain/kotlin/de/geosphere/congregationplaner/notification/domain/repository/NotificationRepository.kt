package de.geosphere.congregationplaner.notification.domain.repository

import de.geosphere.congregationplaner.notification.domain.model.AppNotification
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef

/** Zugriff auf die Benachrichtigungen einer Versammlung (`…/congregation/{id}/notifications/{id}`). */
interface NotificationRepository {
    /** Legt die Benachrichtigung mit [senderUid] als Absender an; die Zeit setzt der Server. */
    suspend fun send(congregation: CongregationRef, senderUid: String, notification: AppNotification): Result<Unit>
}
