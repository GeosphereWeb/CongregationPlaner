package de.geosphere.congregationplaner.organization.data

import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.FakeResponse
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.RecordedRequest
import de.geosphere.congregationplaner.testing.withFirestoreRestConfig
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirestoreRestOrganizationRepositoryTest {
    @Test
    fun `listUnits reads the collection without sign in`() = runWithServer({
        FakeResponse(
            200,
            """{"documents":[
                {"name":"x/region/EMEA/subregion/europe","fields":{"name":{"stringValue":"Europa"}}},
                {"name":"x/region/EMEA/subregion/africa"}]}""",
        )
    }, token = null) { server, repository ->
        val units = repository.listUnits(OrgLevel.SUBREGION, listOf("EMEA")).getOrThrow()

        assertEquals(listOf(OrgUnit("europe", "Europa"), OrgUnit("africa")), units)
        val request = server.requests.single()
        assertEquals("GET", request.method)
        assertEquals("${FakeFirestoreRestServer.DOCUMENTS_PATH}/region/EMEA/subregion", request.path)
        assertNull(request.authorization)
    }

    @Test
    fun `listUnits sends the token when signed in and handles empty collections`() = runWithServer({
        FakeResponse(200, "{}")
    }) { server, repository ->
        assertTrue(repository.listUnits(OrgLevel.REGION, emptyList()).getOrThrow().isEmpty())
        assertEquals("Bearer token-123", server.requests.single().authorization)
    }

    @Test
    fun `listUnits reports http errors and documents without names`() {
        runWithServer({ FakeResponse(403) }) { _, repository ->
            assertIs<IOException>(repository.listUnits(OrgLevel.REGION, emptyList()).exceptionOrNull())
        }
        runWithServer({ FakeResponse(200, """{"documents":[{"fields":{}}]}""") }) { _, repository ->
            assertIs<IOException>(repository.listUnits(OrgLevel.REGION, emptyList()).exceptionOrNull())
        }
    }

    @Test
    fun `desktop factories create rest repository and preferences storage`() {
        assertIs<FirestoreRestOrganizationRepository>(createOrganizationRepository())
        val storage = createKeyValueStorage()
        val key = "test_${System.nanoTime()}"
        storage.putString(key, "value")
        assertEquals("value", storage.getString(key))
        storage.remove(key)
        assertNull(storage.getString(key))
    }

    private fun runWithServer(
        responder: (RecordedRequest) -> FakeResponse,
        token: String? = "token-123",
        block: suspend (FakeFirestoreRestServer, FirestoreRestOrganizationRepository) -> Unit,
    ) {
        FakeFirestoreRestServer(responder).use { server ->
            withFirestoreRestConfig(server) {
                runBlocking { block(server, FirestoreRestOrganizationRepository(idTokenProvider = { token })) }
            }
        }
    }
}
