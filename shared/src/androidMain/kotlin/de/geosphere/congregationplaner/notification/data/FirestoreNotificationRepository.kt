package de.geosphere.congregationplaner.notification.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import de.geosphere.congregationplaner.auth.data.AndroidFirestoreProvider
import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.notification.domain.model.AppNotification
import de.geosphere.congregationplaner.notification.domain.model.ReceivedNotification
import de.geosphere.congregationplaner.notification.domain.repository.NotificationRepository
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import kotlin.time.Instant

actual fun createNotificationRepository(): NotificationRepository =
    FirestoreNotificationRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreNotificationRepository(private val firestore: FirebaseFirestore) : NotificationRepository {
    override suspend fun send(
        congregation: CongregationRef,
        senderUid: String,
        notification: AppNotification,
    ): Result<Unit> = runFirestoreCatching("send notification") {
        firestore.collection(congregation.notificationsPath)
            .add(notification.fieldValues(senderUid) + (CREATED_AT_FIELD to FieldValue.serverTimestamp()))
            .awaitResult()
        Unit
    }

    override suspend fun fetchSince(
        congregation: CongregationRef,
        groups: Set<EventAudience>,
        roles: Set<CongregationRole>,
        since: Instant,
    ): Result<List<ReceivedNotification>> = runFirestoreCatching("fetch notifications") {
        firestore.collection(congregation.notificationsPath)
            .whereArrayContainsAny(AUDIENCE_FIELD, audienceValues(groups, roles))
            .whereGreaterThan(CREATED_AT_FIELD, Timestamp(since.epochSeconds, since.nanosecondsOfSecond))
            .orderBy(CREATED_AT_FIELD, Query.Direction.ASCENDING)
            .limit(FETCH_LIMIT)
            .get()
            .awaitResult()
            .documents
            .mapNotNull(::toReceivedNotification)
    }
}

private fun toReceivedNotification(document: DocumentSnapshot): ReceivedNotification? = receivedNotificationOf(
    id = document.id,
    createdAt = document.getTimestamp(CREATED_AT_FIELD)?.let { Instant.fromEpochSeconds(it.seconds, it.nanoseconds) },
    field = document::getString,
)
