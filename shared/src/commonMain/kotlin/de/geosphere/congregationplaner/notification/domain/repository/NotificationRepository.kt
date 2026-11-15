package de.geosphere.congregationplaner.notification.domain.repository

import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.notification.domain.model.AppNotification
import de.geosphere.congregationplaner.notification.domain.model.ReceivedNotification
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import kotlin.time.Instant

/** Zugriff auf die Benachrichtigungen einer Versammlung (`…/congregation/{id}/notifications/{id}`). */
interface NotificationRepository {
    /** Legt die Benachrichtigung mit [senderUid] als Absender an; die Zeit setzt der Server. */
    suspend fun send(congregation: CongregationRef, senderUid: String, notification: AppNotification): Result<Unit>

    /**
     * Die Benachrichtigungen nach [since], die sich an eine der [groups] oder [roles] richten, nach Zeit sortiert.
     * Mitglieder dürfen nur so abfragen, und nur mit ihren eigenen Gruppen und Rollen (ADR-0035).
     */
    suspend fun fetchSince(
        congregation: CongregationRef,
        groups: Set<EventAudience>,
        roles: Set<CongregationRole>,
        since: Instant,
    ): Result<List<ReceivedNotification>>
}
