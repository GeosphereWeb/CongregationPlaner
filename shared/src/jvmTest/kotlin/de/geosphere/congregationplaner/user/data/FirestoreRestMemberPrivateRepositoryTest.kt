package de.geosphere.congregationplaner.user.data

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.FakeResponse
import de.geosphere.congregationplaner.testing.withFirestoreRestConfig
import de.geosphere.congregationplaner.user.domain.model.EmergencyContact
import de.geosphere.congregationplaner.user.domain.model.MemberPrivateDetails
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirestoreRestMemberPrivateRepositoryTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin")
    private val privatePath = "${FakeFirestoreRestServer.DOCUMENTS_PATH}/${congregation.memberPrivatePath}"

    @Test
    fun `observe maps address and contacts and skips entries that are no map`() = runWithServer({
        FakeResponse(
            200,
            """{"name":"x/memberPrivate/uid-1","fields":{
                "street":{"stringValue":"Hauptstr. 1"},"postalCode":{"stringValue":"10115"},
                "city":{"stringValue":"Berlin"},
                "emergencyContacts":{"arrayValue":{"values":[
                    {"mapValue":{"fields":{"name":{"stringValue":"Anna"},"relation":{"stringValue":"Ehefrau"},
                        "phone":{"stringValue":"0170"}}}},
                    {"stringValue":"kaputt"},
                    {"mapValue":{"fields":{"name":{"stringValue":"Ben"},"phone":{"stringValue":"030"}}}}]}}}}""",
        )
    }) { server, repository ->
        val details = repository.observe(congregation, "uid-1").first()

        assertEquals(
            MemberPrivateDetails(
                street = "Hauptstr. 1",
                postalCode = "10115",
                city = "Berlin",
                emergencyContacts = listOf(
                    EmergencyContact("Anna", "Ehefrau", "0170"),
                    EmergencyContact("Ben", "", "030"),
                ),
            ),
            details,
        )
        assertEquals("$privatePath/uid-1", server.requests.single().path)
    }

    @Test
    fun `observe emits null while nothing was saved`() = runWithServer({ FakeResponse(404, "{}") }) { _, repository ->
        assertNull(repository.observe(congregation, "uid-1").first())
    }

    @Test
    fun `save replaces the document and leaves out an empty relation`() = runWithServer({
        FakeResponse(200, "{}")
    }) { server, repository ->
        val details = MemberPrivateDetails(
            street = "Hauptstr. 1",
            emergencyContacts = listOf(EmergencyContact("Anna", "", "0170")),
        )

        repository.save(congregation, "uid-1", details).getOrThrow()

        val request = server.requests.single()
        assertEquals("PATCH", request.method)
        assertEquals("$privatePath/uid-1", request.path)
        assertTrue(request.body.contains(""""street":{"stringValue":"Hauptstr. 1"}"""))
        assertTrue(request.body.contains(""""name":{"stringValue":"Anna"}"""))
        assertTrue(request.body.contains(""""phone":{"stringValue":"0170"}"""))
        assertFalse(request.body.contains("relation"))
    }

    @Test
    fun `delete removes the document and reports failures`() {
        runWithServer({ FakeResponse(200, "{}") }) { server, repository ->
            repository.delete(congregation, "uid-1").getOrThrow()
            assertEquals("DELETE", server.requests.single().method)
        }
        runWithServer({ FakeResponse(403, "{}") }) { _, repository ->
            assertTrue(repository.delete(congregation, "uid-1").isFailure)
        }
    }

    private fun runWithServer(
        responder: (FakeFirestoreRestServer.RecordedRequest) -> FakeResponse,
        block: suspend (FakeFirestoreRestServer, FirestoreRestMemberPrivateRepository) -> Unit,
    ) {
        FakeFirestoreRestServer(responder).use { server ->
            withFirestoreRestConfig(server) {
                runBlocking { block(server, FirestoreRestMemberPrivateRepository(idTokenProvider = { "token-123" })) }
            }
        }
    }
}
