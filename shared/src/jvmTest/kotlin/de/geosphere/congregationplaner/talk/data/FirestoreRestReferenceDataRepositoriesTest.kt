package de.geosphere.congregationplaner.talk.data

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.FakeResponse
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.RecordedRequest
import de.geosphere.congregationplaner.testing.withFirestoreRestConfig
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FirestoreRestReferenceDataRepositoriesTest {
    private val documents = FakeFirestoreRestServer.DOCUMENTS_PATH

    @Test
    fun `saveTalk patches the document of the number with title and active flag`() = runWithServer({
        FakeResponse(200, "{}")
    }) { server ->
        val talk = Talk(12, "Der Weg zum Glück", active = false)

        assertEquals(talk, FirestoreRestTalkCatalogRepository(idTokenProvider = { "t" }).save(talk).getOrThrow())

        val request = server.requests.single()
        assertEquals("PATCH", request.method)
        assertEquals("$documents/$TALK_CATALOG_COLLECTION_PATH/12", request.path)
        assertTrue(request.body.contains(""""title":{"stringValue":"Der Weg zum Glück"}"""))
        assertTrue(request.body.contains(""""active":{"booleanValue":false}"""))
    }

    @Test
    fun `deleteTalk deletes the document and reports failures`() = runWithServer({ request ->
        if (request.path.endsWith("/12")) FakeResponse(200, "{}") else FakeResponse(403)
    }) { server ->
        val repository = FirestoreRestTalkCatalogRepository(idTokenProvider = { "t" })

        assertTrue(repository.delete(12).isSuccess)
        assertEquals("DELETE", server.requests.single().method)
        assertTrue(repository.delete(13).isFailure)
    }

    @Test
    fun `saveWeekEvent posts a new event and patches an existing one`() = runWithServer({ request ->
        if (request.method == "POST") {
            FakeResponse(
                200,
                """{"name":"x/weekEvents/new1","fields":{
                    "name":{"stringValue":"Kongress"},"cancelsTalk":{"booleanValue":true}}}""",
            )
        } else {
            FakeResponse(200, "{}")
        }
    }) { server ->
        val repository = FirestoreRestWeekEventRepository(idTokenProvider = { "t" })

        val created = repository.save(WeekEvent("", "Kongress", cancelsTalk = true)).getOrThrow()
        assertEquals(WeekEvent("new1", "Kongress", cancelsTalk = true), created)
        val post = server.requests.last()
        assertEquals("POST", post.method)
        assertEquals("$documents/$WEEK_EVENTS_COLLECTION_PATH", post.path)
        assertTrue(post.body.contains(""""cancelsTalk":{"booleanValue":true}"""))

        val existing = WeekEvent("ev1", "Besuch")
        assertEquals(existing, repository.save(existing).getOrThrow())
        val patch = server.requests.last()
        assertEquals("PATCH", patch.method)
        assertEquals("$documents/$WEEK_EVENTS_COLLECTION_PATH/ev1", patch.path)
    }

    @Test
    fun `deleteWeekEvent deletes the document and reports failures`() = runWithServer({ request ->
        if (request.path.endsWith("/ev1")) FakeResponse(200, "{}") else FakeResponse(403)
    }) { server ->
        val repository = FirestoreRestWeekEventRepository(idTokenProvider = { "t" })

        assertTrue(repository.delete("ev1").isSuccess)
        assertEquals("DELETE", server.requests.single().method)
        assertTrue(repository.delete("ev2").isFailure)
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
