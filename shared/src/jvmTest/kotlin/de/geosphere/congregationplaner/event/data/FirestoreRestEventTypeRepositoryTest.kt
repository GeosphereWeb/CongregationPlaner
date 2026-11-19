package de.geosphere.congregationplaner.event.data

import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.FakeResponse
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.RecordedRequest
import de.geosphere.congregationplaner.testing.withFirestoreRestConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FirestoreRestEventTypeRepositoryTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "de", "609a", "berlin")
    private val typesPath = "${FakeFirestoreRestServer.DOCUMENTS_PATH}/${congregation.eventTypesPath}"
    private val repository = FirestoreRestEventTypeRepository(idTokenProvider = { "token-123" })
    private val congress = EventType("kk", "Kreiskongress", cancelsTalk = true, icon = "ic_cp_arena_duo")

    @Test
    fun `observe maps the documents with defaults for missing fields`() = runWithServer({
        FakeResponse(
            200,
            """{"documents":[
                {"name":"x/eventTypes/kk","fields":{"name":{"stringValue":"Kreiskongress"},
                    "cancelsTalk":{"booleanValue":true},"icon":{"stringValue":"ic_cp_arena_duo"}}},
                {"name":"x/eventTypes/x","fields":{}}]}""",
        )
    }) { server ->
        assertEquals(listOf(congress, EventType("x")), repository.observeEventTypes(congregation).first())
        assertEquals(typesPath, server.requests.single().path)
    }

    @Test
    fun `save posts a new type and patches an existing one`() = runWithServer({ request ->
        if (request.method ==
            "POST"
        ) {
            FakeResponse(200, """{"name":"x/eventTypes/new1","fields":{}}""")
        } else {
            FakeResponse(200, "{}")
        }
    }) { server ->
        assertEquals(congress.copy(id = "new1"), repository.save(congregation, congress.copy(id = "")).getOrThrow())
        val post = server.requests.last()
        assertEquals("POST", post.method)
        assertEquals(typesPath, post.path)
        assertTrue(post.body.contains(""""name":{"stringValue":"Kreiskongress"}"""))
        assertTrue(post.body.contains(""""cancelsTalk":{"booleanValue":true}"""))
        assertTrue(post.body.contains(""""icon":{"stringValue":"ic_cp_arena_duo"}"""))

        assertEquals(congress, repository.save(congregation, congress).getOrThrow())
        val patch = server.requests.last()
        assertEquals("PATCH", patch.method)
        assertEquals("$typesPath/kk", patch.path)
    }

    @Test
    fun `delete removes the document and reports failures`() = runWithServer({ request ->
        if (request.path.endsWith("/kk")) FakeResponse(200, "{}") else FakeResponse(403)
    }) { server ->
        assertTrue(repository.delete(congregation, "kk").isSuccess)
        assertEquals("DELETE", server.requests.single().method)
        assertTrue(repository.delete(congregation, "other").isFailure)
    }

    @Test
    fun `the desktop factory creates the rest repository`() {
        assertIs<FirestoreRestEventTypeRepository>(createEventTypeRepository())
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
