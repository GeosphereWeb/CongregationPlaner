package de.geosphere.congregationplaner.organization.data

import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.model.OrgUnitException
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.FakeResponse
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.RecordedRequest
import de.geosphere.congregationplaner.testing.withFirestoreRestConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FirestoreRestOrgUnitRepositoryTest {
    private val branch = listOf("EMEA", "europe", "central-europe", "de")
    private val circuitsPath =
        "${FakeFirestoreRestServer.DOCUMENTS_PATH}/region/EMEA/subregion/europe/branch/central-europe/country/de/circuit"

    @Test
    fun `observe maps the documents of the level`() = runWithServer({
        FakeResponse(
            200,
            """{"documents":[
                {"name":"x/c1","fields":{"name":{"stringValue":"Kreis Nord"},"overseer_name":{"stringValue":"Max"}}},
                {"name":"x/c2"}]}""",
        )
    }) { server, repository ->
        assertEquals(
            listOf(OrgUnit("c1", "Kreis Nord", "Max"), OrgUnit("c2")),
            repository.observe(OrgLevel.CIRCUIT, branch).first(),
        )
        assertEquals(circuitsPath, server.requests.single().path)
        assertEquals("Bearer token-123", server.requests.single().authorization)
    }

    @Test
    fun `create posts without document id and returns the generated id`() = runWithServer({
        FakeResponse(200, """{"name":"x/region/generated-1","fields":{}}""")
    }) { server, repository ->
        val created = repository.create(OrgLevel.REGION, emptyList(), OrgUnit(name = "EMEA", overseerName = "x"))

        assertEquals(OrgUnit("generated-1", "EMEA", "x"), created.getOrThrow())
        val request = server.requests.single()
        assertEquals("POST", request.method)
        assertEquals("${FakeFirestoreRestServer.DOCUMENTS_PATH}/region", request.path)
        assertEquals(null, request.query)
        assertTrue(request.body.contains(""""name":{"stringValue":"EMEA"}"""))
        assertFalse(request.body.contains("overseer_name"))
    }

    @Test
    fun `update patches only the fields of the level of an existing unit`() = runWithServer({
        FakeResponse(200, "{}")
    }) { server, repository ->
        val circuit = OrgUnit("c1", "Kreis Nord", "Max")

        assertEquals(circuit, repository.update(OrgLevel.CIRCUIT, branch, circuit).getOrThrow())
        val request = server.requests.single()
        assertEquals("PATCH", request.method)
        assertEquals("$circuitsPath/c1", request.path)
        assertEquals(
            "updateMask.fieldPaths=name&updateMask.fieldPaths=overseer_name&currentDocument.exists=true",
            request.query,
        )
        assertTrue(request.body.contains(""""overseer_name":{"stringValue":"Max"}"""))
    }

    @Test
    fun `delete removes an empty unit after checking its children`() = runWithServer({
        FakeResponse(200, "{}")
    }) { server, repository ->
        repository.delete(OrgLevel.CIRCUIT, branch, OrgUnit("c1", "Kreis Nord")).getOrThrow()

        assertEquals(listOf("GET", "DELETE"), server.requests.map { it.method })
        assertEquals("$circuitsPath/c1/congregation", server.requests.first().path)
        assertEquals("pageSize=1", server.requests.first().query)
        assertEquals("$circuitsPath/c1", server.requests.last().path)
    }

    @Test
    fun `delete refuses units with children`() = runWithServer({
        FakeResponse(200, """{"documents":[{"name":"x/k1"}]}""")
    }) { server, repository ->
        val error = repository.delete(OrgLevel.CIRCUIT, branch, OrgUnit("c1", "Kreis Nord")).exceptionOrNull()

        assertIs<OrgUnitException.HasChildren>(error)
        assertEquals(listOf("GET"), server.requests.map { it.method })
    }

    @Test
    fun `requests need a sign in and report http errors`() {
        runWithServer({ FakeResponse(200, "{}") }, token = null) { server, repository ->
            assertIs<IllegalStateException>(
                repository.create(OrgLevel.REGION, emptyList(), OrgUnit(name = "EMEA")).exceptionOrNull(),
            )
            assertTrue(server.requests.isEmpty())
        }
        runWithServer({ FakeResponse(403) }) { _, repository ->
            assertIs<IOException>(
                repository.update(OrgLevel.REGION, emptyList(), OrgUnit("r1", "EMEA")).exceptionOrNull(),
            )
        }
    }

    @Test
    fun `desktop factory creates rest repository`() {
        assertIs<FirestoreRestOrgUnitRepository>(createOrgUnitRepository())
    }

    private fun runWithServer(
        responder: (RecordedRequest) -> FakeResponse,
        token: String? = "token-123",
        block: suspend (FakeFirestoreRestServer, FirestoreRestOrgUnitRepository) -> Unit,
    ) {
        FakeFirestoreRestServer(responder).use { server ->
            withFirestoreRestConfig(server) {
                runBlocking { block(server, FirestoreRestOrgUnitRepository(idTokenProvider = { token })) }
            }
        }
    }
}
