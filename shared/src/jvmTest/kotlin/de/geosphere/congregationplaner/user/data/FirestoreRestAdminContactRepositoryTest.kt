package de.geosphere.congregationplaner.user.data

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.FakeResponse
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.RecordedRequest
import de.geosphere.congregationplaner.testing.withFirestoreRestConfig
import de.geosphere.congregationplaner.user.domain.model.AdminContact
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FirestoreRestAdminContactRepositoryTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin")
    private val contactsPath = "${FakeFirestoreRestServer.DOCUMENTS_PATH}/${congregation.adminContactsPath}"

    @Test
    fun `getAdminContacts lists the contacts`() = runWithServer({
        FakeResponse(
            200,
            """{"documents":[
                {"name":"x/adminContacts/a1","fields":{
                    "firstName":{"stringValue":"Anna"},"lastName":{"stringValue":"Admin"}}},
                {"name":"x/adminContacts/a2","fields":{"lastName":{"stringValue":"Ohne Vorname"}}}]}""",
        )
    }) { server, repository ->
        val contacts = repository.getAdminContacts(congregation).getOrThrow()

        assertEquals(listOf(AdminContact("a1", "Anna", "Admin"), AdminContact("a2", "", "Ohne Vorname")), contacts)
        assertEquals("GET", server.requests.single().method)
        assertEquals(contactsPath, server.requests.single().path)
    }

    @Test
    fun `getAdminContacts without documents is an empty list`() = runWithServer({
        FakeResponse(200, "{}")
    }) { _, repository ->
        assertEquals(emptyList(), repository.getAdminContacts(congregation).getOrThrow())
    }

    @Test
    fun `getAdminContacts reports failures`() = runWithServer({ FakeResponse(403) }) { _, repository ->
        assertIs<IOException>(repository.getAdminContacts(congregation).exceptionOrNull())
    }

    @Test
    fun `putAdminContact replaces the document with the name only`() = runWithServer({
        FakeResponse(200, "{}")
    }) { server, repository ->
        repository.putAdminContact(congregation, AdminContact("a1", "Anna", "Admin")).getOrThrow()

        val request = server.requests.single()
        assertEquals("PATCH", request.method)
        assertEquals("$contactsPath/a1", request.path)
        assertTrue(""""firstName":{"stringValue":"Anna"}""" in request.body)
        assertTrue(""""lastName":{"stringValue":"Admin"}""" in request.body)
    }

    @Test
    fun `putAdminContact reports failures`() = runWithServer({ FakeResponse(403) }) { _, repository ->
        assertIs<IOException>(repository.putAdminContact(congregation, AdminContact("a1", "A", "B")).exceptionOrNull())
    }

    @Test
    fun `deleteAdminContact deletes the document`() = runWithServer({ FakeResponse(200, "{}") }) { server, repository ->
        repository.deleteAdminContact(congregation, "a1").getOrThrow()

        assertEquals("DELETE", server.requests.single().method)
        assertEquals("$contactsPath/a1", server.requests.single().path)
    }

    @Test
    fun `deleteAdminContact reports failures`() = runWithServer({ FakeResponse(403) }) { _, repository ->
        assertIs<IOException>(repository.deleteAdminContact(congregation, "a1").exceptionOrNull())
    }

    @Test
    fun `desktop factory creates rest admin contact repository`() {
        assertIs<FirestoreRestAdminContactRepository>(createAdminContactRepository())
    }

    private fun runWithServer(
        responder: (RecordedRequest) -> FakeResponse,
        block: suspend (FakeFirestoreRestServer, FirestoreRestAdminContactRepository) -> Unit,
    ) {
        FakeFirestoreRestServer(responder).use { server ->
            withFirestoreRestConfig(server) {
                runBlocking { block(server, FirestoreRestAdminContactRepository(idTokenProvider = { "token-123" })) }
            }
        }
    }
}
