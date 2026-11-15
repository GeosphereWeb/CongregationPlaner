package de.geosphere.congregationplaner.notification.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.notification.domain.model.AppNotification
import de.geosphere.congregationplaner.notification.domain.model.NotificationCategory
import de.geosphere.congregationplaner.notification.domain.model.ReceivedNotification
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
import kotlin.time.Instant

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

    @Test
    fun `fetchSince queries the own audience after the time and skips unknown documents`(): Unit = withMockedLog {
        val since = Instant.parse("2026-11-15T11:00:00.123456Z")
        val query = mockk<Query>()
        every { firestore.collection(congregation.notificationsPath) } returns collection
        every { collection.whereArrayContainsAny("audience", listOf("ALL", "SISTERS", "TALK_HELPER")) } returns query
        every { query.whereGreaterThan("createdAt", Timestamp(since.epochSeconds, since.nanosecondsOfSecond)) } returns
            query
        every { query.orderBy("createdAt", Query.Direction.ASCENDING) } returns query
        every { query.limit(20) } returns query
        val created = Timestamp(1_763_204_400, 500)
        val snapshot = mockk<QuerySnapshot> {
            every { documents } returns listOf(
                document("n1", "TALK_PLANNING", created),
                document("n2", "UNKNOWN", created),
                document("n3", "TALK_PLANNING", null),
            )
        }
        every { query.get() } returns successTask(snapshot)

        val groups = setOf(EventAudience.SISTERS, EventAudience.ALL)
        val roles = setOf(CongregationRole.TALK_HELPER)
        val fetched = runBlocking { repository.fetchSince(congregation, groups, roles, since).getOrThrow() }

        assertEquals(
            listOf(
                ReceivedNotification(
                    id = "n1",
                    category = NotificationCategory.TALK_PLANNING,
                    title = "Redner informiert",
                    body = "Vortrag am 22.11.2026",
                    senderUid = "helper",
                    createdAt = Instant.fromEpochSeconds(1_763_204_400, 500),
                ),
            ),
            fetched,
        )

        val error = IllegalStateException("permission denied")
        every { query.get() } returns failedTask(error)
        runBlocking {
            assertFailure(error, repository.fetchSince(congregation, groups, roles, since))
        }
    }

    private fun document(id: String, category: String, createdAt: Timestamp?): DocumentSnapshot = mockk {
        every { this@mockk.id } returns id
        every { getString("category") } returns category
        every { getString("title") } returns "Redner informiert"
        every { getString("body") } returns "Vortrag am 22.11.2026"
        every { getString("senderUid") } returns "helper"
        every { getTimestamp("createdAt") } returns createdAt
    }
}
