package de.geosphere.congregationplaner.circuit.data

import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.circuit.domain.model.CircuitException
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.FakeResponse
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.RecordedRequest
import de.geosphere.congregationplaner.testing.withFirestoreRestConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FirestoreRestCircuitRepositoryTest {
    private val collectionPath = "${FakeFirestoreRestServer.DOCUMENTS_PATH}/$CIRCUIT_COLLECTION_PATH"

    @Test
    fun `observeAll maps firestore documents to circuits`() = runWithServer({
        FakeResponse(200, documentsJson(circuitDocument("609a", "Nord", "Max"), """{"name":"x/610b"}"""))
    }) { server, repository ->
        assertEquals(
            listOf(Circuit("609a", "Nord", "Max"), Circuit("610b", "", "")),
            repository.observeAll().first(),
        )
        val request = server.requests.single()
        assertEquals("GET", request.method)
        assertEquals(collectionPath, request.path)
        assertEquals("pageSize=1000", request.query)
        assertEquals("Bearer token-123", request.authorization)
    }

    @Test
    fun `observeAll returns empty list and rejects invalid data`() {
        runWithServer({ FakeResponse(200, "{}") }) { _, repository ->
            assertEquals(emptyList(), repository.observeAll().first())
        }
        runWithServer({
            FakeResponse(200, documentsJson("""{"name":"x/1","fields":{"name":{"integerValue":"5"}}}"""))
        }) { _, repository ->
            assertFailsWith<IllegalArgumentException> { repository.observeAll().first() }
        }
        runWithServer({ FakeResponse(200, """{"documents":[{"fields":{}}]}""") }) { _, repository ->
            assertFailsWith<IOException> { repository.observeAll().first() }
        }
    }

    @Test
    fun `create posts document with chosen id`() = runWithServer({ FakeResponse(200, "{}") }) { server, repository ->
        val circuit = Circuit(id = "609a", name = "Nord", overseerName = "Max")

        assertEquals(circuit, repository.create(circuit).getOrThrow())

        val request = server.requests.single()
        assertEquals("POST", request.method)
        assertEquals(collectionPath, request.path)
        assertEquals("documentId=609a", request.query)
        assertTrue(request.body.contains(""""name":{"stringValue":"Nord"}"""))
        assertTrue(request.body.contains(""""overseer_name":{"stringValue":"Max"}"""))
    }

    @Test
    fun `create maps conflict to already exists and reports other failures`() {
        runWithServer({ FakeResponse(409) }) { _, repository ->
            assertIs<CircuitException.AlreadyExists>(repository.create(Circuit("609a", "Nord")).exceptionOrNull())
        }
        runWithServer({ FakeResponse(500) }) { _, repository ->
            assertIs<IOException>(repository.create(Circuit("609a", "Nord")).exceptionOrNull())
        }
        runWithServer({ FakeResponse(200) }, token = null) { server, repository ->
            assertIs<IllegalStateException>(repository.create(Circuit("609a", "Nord")).exceptionOrNull())
            assertTrue(server.requests.isEmpty())
        }
    }

    @Test
    fun `update patches existing document only`() = runWithServer({ FakeResponse(200, "{}") }) { server, repository ->
        val circuit = Circuit(id = "609a", name = "Nord")

        assertEquals(circuit, repository.update(circuit).getOrThrow())

        val request = server.requests.single()
        assertEquals("PATCH", request.method)
        assertEquals("$collectionPath/609a", request.path)
        val query = request.query.orEmpty()
        assertTrue(query.contains("updateMask.fieldPaths=name"))
        assertTrue(query.contains("updateMask.fieldPaths=overseer_name"))
        assertTrue(query.contains("currentDocument.exists=true"))
    }

    @Test
    fun `update reports http errors`() = runWithServer({ FakeResponse(404) }) { _, repository ->
        assertIs<IOException>(repository.update(Circuit("609a", "Nord")).exceptionOrNull())
    }

    @Test
    fun `delete removes empty circuit after checking for congregations`() = runWithServer(
        { FakeResponse(200, "{}") },
    ) { server, repository ->
        assertTrue(repository.delete("609a").isSuccess)

        val (check, delete) = server.requests
        assertEquals("GET", check.method)
        assertEquals("$collectionPath/609a/$CONGREGATION_SUBCOLLECTION", check.path)
        assertEquals("pageSize=1", check.query)
        assertEquals("DELETE", delete.method)
        assertEquals("$collectionPath/609a", delete.path)
    }

    @Test
    fun `delete refuses circuits that still contain congregations`() = runWithServer({
        FakeResponse(200, documentsJson("""{"name":"x/berlin"}"""))
    }) { server, repository ->
        assertIs<CircuitException.HasCongregations>(repository.delete("609a").exceptionOrNull())
        assertEquals(listOf("GET"), server.requests.map(RecordedRequest::method))
    }

    @Test
    fun `delete rejects blank ids and reports http errors`() {
        runWithServer({ FakeResponse(200) }) { server, repository ->
            assertIs<IllegalArgumentException>(repository.delete(" ").exceptionOrNull())
            assertTrue(server.requests.isEmpty())
        }
        val emptyThenForbidden = { request: RecordedRequest ->
            if (request.method == "GET") FakeResponse(200, "{}") else FakeResponse(403)
        }
        runWithServer(emptyThenForbidden) { _, repository ->
            assertIs<IOException>(repository.delete("609a").exceptionOrNull())
        }
    }

    @Test
    fun `desktop factory creates rest repository`() {
        assertIs<FirestoreRestCircuitRepository>(createCircuitRepository())
    }

    private fun runWithServer(
        responder: (RecordedRequest) -> FakeResponse,
        token: String? = "token-123",
        block: suspend (FakeFirestoreRestServer, FirestoreRestCircuitRepository) -> Unit,
    ) {
        FakeFirestoreRestServer(responder).use { server ->
            withFirestoreRestConfig(server) {
                runBlocking { block(server, FirestoreRestCircuitRepository(idTokenProvider = { token })) }
            }
        }
    }

    private fun circuitDocument(id: String, name: String, overseerName: String): String =
        """{"name":"projects/p/databases/(default)/documents/circuit/$id","fields":{
        "name":{"stringValue":"$name"},
        "overseer_name":{"stringValue":"$overseerName"}}}"""

    private fun documentsJson(vararg documents: String): String = """{"documents":[${documents.joinToString(",")}]}"""
}
