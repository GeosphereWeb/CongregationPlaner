package de.geosphere.congregationplaner.talk.data

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.FakeResponse
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.RecordedRequest
import de.geosphere.congregationplaner.testing.withFirestoreRestConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FirestoreRestExternalSpeakerDirectoryRepositoryTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin")
    private val documents = FakeFirestoreRestServer.DOCUMENTS_PATH
    private val circuitsPath = "$documents/${congregation.externalCircuitsPath}"
    private val congregationsPath = "$documents/${congregation.externalCongregationsPath}"
    private val speakersPath = "$documents/${congregation.visitingSpeakersPath}"

    private fun runWithRepository(
        responder: (RecordedRequest) -> FakeResponse,
        block: suspend (FirestoreRestExternalSpeakerDirectoryRepository, FakeFirestoreRestServer) -> Unit,
    ) = FakeFirestoreRestServer(responder).use { server ->
        withFirestoreRestConfig(server) {
            runBlocking {
                block(FirestoreRestExternalSpeakerDirectoryRepository(idTokenProvider = { "token-123" }), server)
            }
        }
    }

    @Test
    fun `circuits and congregations are listed, created, updated and deleted`() = runWithRepository(
        responder = { request ->
            when {
                request.method == "GET" && request.path == circuitsPath -> FakeResponse(
                    200,
                    """{"documents":[{"name":"x/externalCircuits/c1","fields":{"name":{"stringValue":"Nord"}}}]}""",
                )
                request.method == "GET" -> FakeResponse(
                    200,
                    """{"documents":[{"name":"x/externalCongregations/g1","fields":{
                        "circuitId":{"stringValue":"c1"},"name":{"stringValue":"Altdorf"}}}]}""",
                )
                request.method == "POST" -> FakeResponse(
                    200,
                    """{"name":"x/externalCircuits/new1","fields":{"name":{"stringValue":"Süd"}}}""",
                )
                request.method == "DELETE" && request.path.endsWith("/c9") -> FakeResponse(403)
                else -> FakeResponse(200, "{}")
            }
        },
    ) { repository, server ->
        assertEquals(listOf(ExternalCircuit("c1", "Nord")), repository.observeCircuits(congregation).first())
        assertEquals(
            listOf(ExternalCongregation("g1", "c1", "Altdorf")),
            repository.observeCongregations(congregation).first(),
        )
        assertEquals(congregationsPath, server.requests.last().path)

        val created = repository.saveCircuit(congregation, ExternalCircuit(name = "Süd")).getOrThrow()
        assertEquals(ExternalCircuit("new1", "Süd"), created)
        val post = server.requests.last()
        assertEquals("POST", post.method)
        assertEquals(circuitsPath, post.path)
        assertTrue(post.body.contains(""""name":{"stringValue":"Süd"}"""))

        val updated = ExternalCongregation("g1", "c1", "Neudorf")
        assertEquals(updated, repository.saveCongregation(congregation, updated).getOrThrow())
        val patch = server.requests.last()
        assertEquals("PATCH", patch.method)
        assertEquals("$congregationsPath/g1", patch.path)
        assertTrue(patch.body.contains(""""circuitId":{"stringValue":"c1"}"""))

        assertTrue(repository.deleteCongregation(congregation, "g1").isSuccess)
        assertEquals("DELETE", server.requests.last().method)
        assertTrue(repository.deleteCircuit(congregation, "c9").isFailure)
    }

    @Test
    fun `speakers are read and written with phone, email and talk numbers`() = runWithRepository(
        responder = { request ->
            when (request.method) {
                "GET" -> FakeResponse(
                    200,
                    """{"documents":[
                        {"name":"x/visitingSpeakers/s1","fields":{
                            "congregationId":{"stringValue":"g1"},"lastName":{"stringValue":"Berg"},
                            "firstName":{"stringValue":"Lea"},"phone":{"stringValue":"0170"},
                            "email":{"stringValue":"lea@example.org"},
                            "talkNumbers":{"arrayValue":{"values":[{"integerValue":"12"},{"integerValue":"45"}]}}}},
                        {"name":"x/visitingSpeakers/s2","fields":{}}]}""",
                )
                "POST" -> FakeResponse(200, """{"name":"x/visitingSpeakers/new1","fields":{}}""")
                else -> FakeResponse(200, "{}")
            }
        },
    ) { repository, server ->
        val listed = repository.observeSpeakers(congregation).first()
        assertEquals(
            listOf(
                VisitingSpeaker("s1", "g1", "Berg", "Lea", "0170", "lea@example.org", listOf(12, 45)),
                VisitingSpeaker("s2", "", ""),
            ),
            listed,
        )
        assertEquals(speakersPath, server.requests.first().path)

        val created = repository.saveSpeaker(
            congregation,
            VisitingSpeaker(congregationId = "g1", lastName = "Neu", talkNumbers = listOf(7, 9)),
        ).getOrThrow()
        assertEquals("new1", created.id)
        val body = server.requests.last().body
        assertTrue(
            body.contains(""""talkNumbers":{"arrayValue":{"values":[{"integerValue":"7"},{"integerValue":"9"}]}}"""),
        )
        assertTrue(body.contains(""""phone":{"stringValue":""}"""))

        assertTrue(repository.deleteSpeaker(congregation, "s1").isSuccess)
        assertEquals("$speakersPath/s1", server.requests.last().path)
    }
}
