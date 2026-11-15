package de.geosphere.congregationplaner.notification.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.data.AndroidFirestoreProvider
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.notification.domain.model.AppNotification
import de.geosphere.congregationplaner.notification.domain.repository.NotificationRepository
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef

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
}
