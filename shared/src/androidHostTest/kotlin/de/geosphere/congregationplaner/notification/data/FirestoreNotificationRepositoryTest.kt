package de.geosphere.congregationplaner.notification.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.notification.domain.model.AppNotification
import de.geosphere.congregationplaner.notification.domain.model.NotificationCategory
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.testing.assertFailure
import de.geosphere.congregationplaner.testing.failedTask
import de.geosphere.congregationplaner.testing.successTask
import de.geosphere.congregationplaner.testing.withMockedLog
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class FirestoreNotificationRepositoryTest {
    private val firestore = mockk<FirebaseFirestore>()
    private val collection = mockk<CollectionReference>()
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "de", "609a", "berlin")
    private val repository = FirestoreNotificationRepository(firestore)
    private val notification = AppNotification(
        category = NotificationCategory.TALK_PLANNING,
        title = "Redner informiert",
        body = "Vortrag am 22.11.2026",
        roles = setOf(CongregationRole.TALK_HELPER, CongregationRole.TALK_PLANNER),
    )

    @Test
    fun `send adds the fields with the server time and reports failures`(): Unit = withMockedLog {
        val serverTime = mockk<FieldValue>()
        mockkStatic(FieldValue::class)
        try {
            every { FieldValue.serverTimestamp() } returns serverTime
            every { firestore.collection(congregation.notificationsPath) } returns collection
            val fields = slot<Map<String, Any>>()
            every { collection.add(capture(fields)) } returns successTask(mockk<DocumentReference>())

            runBlocking { assertEquals(Unit, repository.send(congregation, "helper", notification).getOrThrow()) }
            assertEquals(
                mapOf(
                    "category" to "TALK_PLANNING",
                    "title" to "Redner informiert",
                    "body" to "Vortrag am 22.11.2026",
                    "audience" to listOf("TALK_PLANNER", "TALK_HELPER"),
                    "senderUid" to "helper",
                    "createdAt" to serverTime,
                ),
                fields.captured,
            )

            val error = IllegalStateException("permission denied")
            every { collection.add(any()) } returns failedTask(error)
            runBlocking { assertFailure(error, repository.send(congregation, "helper", notification)) }
        } finally {
            unmockkStatic(FieldValue::class)
        }
    }
}
