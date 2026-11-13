package de.geosphere.congregationplaner.user.data

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.FakeResponse
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.RecordedRequest
import de.geosphere.congregationplaner.testing.withFirestoreRestConfig
import de.geosphere.congregationplaner.user.domain.model.MessageTemplate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FirestoreRestMessageTemplateRepositoryTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "de", "609a", "berlin")
    private val documentPath =
        "${FakeFirestoreRestServer.DOCUMENTS_PATH}/${congregation.membersPath}/u1/settings/messageTemplates"

    @Test
    fun `observeTemplates maps the stored list and skips broken entries`() = runWithServer({
        FakeResponse(
            200,
            """{"name":"x/settings/messageTemplates","fields":{"templates":{"arrayValue":{"values":[
                {"mapValue":{"fields":{"name":{"stringValue":"Kurz"},"text":{"stringValue":"Hallo {Vorname}"}}}},
                {"mapValue":{"fields":{"name":{"stringValue":"Ohne Text"}}}}]}}}}""",
        )
    }) { server, repository ->
        assertEquals(
            listOf(MessageTemplate("Kurz", "Hallo {Vorname}")),
            repository.observeTemplates(congregation, "u1").first(),
        )
        assertEquals(documentPath, server.requests.single().path)
    }

    @Test
    fun `observeTemplates is empty without a document`() = runWithServer({ FakeResponse(404) }) { _, repository ->
        assertEquals(emptyList(), repository.observeTemplates(congregation, "u1").first())
    }

    @Test
    fun `saveTemplates replaces the document with the list`() = runWithServer({
        FakeResponse(200, "{}")
    }) { server, repository ->
        repository.saveTemplates(congregation, "u1", listOf(MessageTemplate("Kurz", "Hallo"))).getOrThrow()

        val request = server.requests.single()
        assertEquals("PATCH", request.method)
        assertEquals(documentPath, request.path)
        assertTrue(""""name":{"stringValue":"Kurz"}""" in request.body)
        assertTrue(""""text":{"stringValue":"Hallo"}""" in request.body)
    }

    @Test
    fun `saveTemplates reports failures`() = runWithServer({ FakeResponse(403) }) { _, repository ->
        assertIs<IOException>(
            repository.saveTemplates(congregation, "u1", listOf(MessageTemplate("A", "B"))).exceptionOrNull(),
        )
    }

    @Test
    fun `desktop factory creates rest message template repository`() {
        assertIs<FirestoreRestMessageTemplateRepository>(createMessageTemplateRepository())
    }

    private fun runWithServer(
        responder: (RecordedRequest) -> FakeResponse,
        block: suspend (FakeFirestoreRestServer, FirestoreRestMessageTemplateRepository) -> Unit,
    ) {
        FakeFirestoreRestServer(responder).use { server ->
            withFirestoreRestConfig(server) {
                runBlocking { block(server, FirestoreRestMessageTemplateRepository(idTokenProvider = { "token-123" })) }
            }
        }
    }
}
