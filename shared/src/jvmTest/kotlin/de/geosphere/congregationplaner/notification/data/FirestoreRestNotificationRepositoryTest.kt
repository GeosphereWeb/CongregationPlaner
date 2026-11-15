package de.geosphere.congregationplaner.notification.data

import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.notification.domain.model.AppNotification
import de.geosphere.congregationplaner.notification.domain.model.NotificationCategory
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.FakeResponse
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.RecordedRequest
import de.geosphere.congregationplaner.testing.withFirestoreRestConfig
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class FirestoreRestNotificationRepositoryTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "de", "609a", "berlin")
    private val repository = FirestoreRestNotificationRepository(idTokenProvider = { "token-123" })
    private val notification = AppNotification(
        category = NotificationCategory.TALK_PLANNING,
        title = "Redner informiert",
        body = "Vortrag am 22.11.2026",
        groups = setOf(EventAudience.SISTERS, EventAudience.ALL),
        roles = setOf(CongregationRole.TALK_HELPER, CongregationRole.TALK_PLANNER),
    )

    @Test
    fun `send creates a new document with the server time and reports failures`() = runWithServer({ request ->
        if (request.body.contains("fail")) FakeResponse(403) else FakeResponse(200, "{}")
    }) { server ->
        assertTrue(repository.send(congregation, "helper", notification).isSuccess)

        val request = server.requests.single()
        assertEquals("POST", request.method)
        assertEquals("${FakeFirestoreRestServer.DOCUMENTS_PATH}:commit", request.path)
        assertTrue(request.body.contains("/${congregation.notificationsPath}/"))
        assertTrue(request.body.contains(""""category":{"stringValue":"TALK_PLANNING"}"""))
        assertTrue(request.body.contains(""""title":{"stringValue":"Redner informiert"}"""))
        assertTrue(request.body.contains(""""body":{"stringValue":"Vortrag am 22.11.2026"}"""))
        assertTrue(request.body.contains(""""senderUid":{"stringValue":"helper"}"""))
        assertTrue(
            request.body.contains(
                """"audience":{"arrayValue":{"values":[{"stringValue":"ALL"},{"stringValue":"SISTERS"},""" +
                    """{"stringValue":"TALK_PLANNER"},{"stringValue":"TALK_HELPER"}]}}""",
            ),
        )
        assertTrue(request.body.contains(""""currentDocument":{"exists":false}"""))
        assertTrue(request.body.contains(""""fieldPath":"createdAt","setToServerValue":"REQUEST_TIME""""))

        assertTrue(repository.send(congregation, "fail", notification).isFailure)
    }

    @Test
    fun `the desktop does not fetch notifications`() = runBlocking {
        val result = repository.fetchSince(congregation, setOf(EventAudience.ALL), emptySet(), Instant.DISTANT_PAST)

        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
    }

    private fun runWithServer(
        responder: (RecordedRequest) -> FakeResponse,
        block: suspend (FakeFirestoreRestServer) -> Unit,
    ) {
        FakeFirestoreRestServer(responder).use { server ->
            withFirestoreRestConfig(server) { runBlocking { block(server) } }
        }
    }
}
