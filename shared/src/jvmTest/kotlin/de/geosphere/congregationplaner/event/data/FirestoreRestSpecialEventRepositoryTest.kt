package de.geosphere.congregationplaner.event.data

import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.event.domain.model.SpecialEvent
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.FakeResponse
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.RecordedRequest
import de.geosphere.congregationplaner.testing.withFirestoreRestConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FirestoreRestSpecialEventRepositoryTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "de", "609a", "berlin")
    private val eventsPath = "${FakeFirestoreRestServer.DOCUMENTS_PATH}/${congregation.eventsPath}"
    private val repository = FirestoreRestSpecialEventRepository(idTokenProvider = { "token-123" })
    private val congress = SpecialEvent(
        id = "ev1",
        title = "Kongress",
        startDate = LocalDate(2027, 6, 11),
        endDate = LocalDate(2027, 6, 13),
        audience = setOf(EventAudience.SISTERS, EventAudience.BROTHERS),
        eventTypeId = "kk",
    )

    @Test
    fun `observeAll maps the events and skips those without a valid start`() = runWithServer({
        FakeResponse(
            200,
            """{"documents":[
                {"name":"x/events/ev1","fields":{"title":{"stringValue":"Kongress"},"eventTypeId":{"stringValue":"kk"},
                    "startDate":{"stringValue":"2027-06-11"},"endDate":{"stringValue":"2027-06-13"},
                    "audience":{"arrayValue":{"values":[{"stringValue":"BROTHERS"},{"stringValue":"SISTERS"},
                        {"stringValue":"UNKNOWN"}]}}}},
                {"name":"x/events/ev2","fields":{"title":{"stringValue":"Besuch"},
                    "startDate":{"stringValue":"2027-03-02"},
                    "audience":{"arrayValue":{"values":[{"stringValue":"ALL"}]}}}},
                {"name":"x/events/ev3","fields":{"startDate":{"stringValue":"kein Datum"}}}]}""",
        )
    }) { server ->
        assertEquals(
            listOf(
                congress,
                SpecialEvent("ev2", "Besuch", LocalDate(2027, 3, 2), audience = setOf(EventAudience.ALL)),
            ),
            repository.observeAll(congregation).first(),
        )
        assertEquals(eventsPath, server.requests.single().path)
    }

    @Test
    fun `observeFor runs a query for the groups and skips the read time row`() = runWithServer({
        FakeResponse(
            200,
            """[{"document":{"name":"x/events/ev1","fields":{"title":{"stringValue":"Kongress"},"eventTypeId":{"stringValue":"kk"},
                    "startDate":{"stringValue":"2027-06-11"},"endDate":{"stringValue":"2027-06-13"},
                    "audience":{"arrayValue":{"values":[{"stringValue":"BROTHERS"},{"stringValue":"SISTERS"}]}}}},
                "readTime":"t"},
                {"readTime":"t"}]""",
        )
    }) { server ->
        assertEquals(
            listOf(congress),
            repository.observeFor(congregation, setOf(EventAudience.SISTERS, EventAudience.ALL)).first(),
        )
        val request = server.requests.single()
        assertEquals("POST", request.method)
        assertEquals("${FakeFirestoreRestServer.DOCUMENTS_PATH}/${congregation.documentPath}:runQuery", request.path)
        assertTrue(request.body.contains(""""collectionId":"events""""))
        assertTrue(request.body.contains(""""op":"ARRAY_CONTAINS_ANY""""))
        assertTrue(
            request.body.contains(""""values":[{"stringValue":"ALL"},{"stringValue":"SISTERS"}]"""),
        )
    }

    @Test
    fun `save posts a new event and patches an existing one`() = runWithServer({ request ->
        if (request.method ==
            "POST"
        ) {
            FakeResponse(200, """{"name":"x/events/new1","fields":{}}""")
        } else {
            FakeResponse(200, "{}")
        }
    }) { server ->
        assertEquals(congress.copy(id = "new1"), repository.save(congregation, congress.copy(id = "")).getOrThrow())
        val post = server.requests.last()
        assertEquals("POST", post.method)
        assertEquals(eventsPath, post.path)
        assertTrue(post.body.contains(""""startDate":{"stringValue":"2027-06-11"}"""))
        assertTrue(post.body.contains(""""endDate":{"stringValue":"2027-06-13"}"""))
        assertTrue(post.body.contains(""""eventTypeId":{"stringValue":"kk"}"""))
        assertTrue(
            post.body.contains(
                """"audience":{"arrayValue":{"values":[{"stringValue":"BROTHERS"},{"stringValue":"SISTERS"}]}}""",
            ),
        )

        assertEquals(congress, repository.save(congregation, congress).getOrThrow())
        val patch = server.requests.last()
        assertEquals("PATCH", patch.method)
        assertEquals("$eventsPath/ev1", patch.path)
    }

    @Test
    fun `delete removes the document and reports failures`() = runWithServer({ request ->
        if (request.path.endsWith("/ev1")) FakeResponse(200, "{}") else FakeResponse(403)
    }) { server ->
        assertTrue(repository.delete(congregation, "ev1").isSuccess)
        assertEquals("DELETE", server.requests.single().method)
        assertTrue(repository.delete(congregation, "ev2").isFailure)
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
