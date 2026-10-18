package de.geosphere.congregationplaner.user.data

import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.FakeResponse
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.RecordedRequest
import de.geosphere.congregationplaner.testing.withFirestoreRestConfig
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirestoreRestUserRepositoryTest {
    private val usersPath = "${FakeFirestoreRestServer.DOCUMENTS_PATH}/$USERS_COLLECTION_PATH"

    private val authUser = object : FirebaseUser {
        override val uid: String = "uid-1"
        override val email: String? = "user@example.com"
        override val displayName: String? = null
        override val idToken: String? = "token-123"
    }

    @Test
    fun `getOrCreateUser returns existing user document`() = runWithServer({
        FakeResponse(200, userDocument(approved = true, role = "ADMIN"))
    }) { server, repository ->
        val user = repository.getOrCreateUser(authUser).getOrThrow()

        assertEquals(
            AppUser(
                uid = "uid-1",
                email = "user@example.com",
                displayName = "User",
                approved = true,
                role = UserRole.ADMIN,
            ),
            user,
        )
        val request = server.requests.single()
        assertEquals("GET", request.method)
        assertEquals("$usersPath/uid-1", request.path)
        assertEquals("Bearer token-123", request.authorization)
    }

    @Test
    fun `getOrCreateUser creates unapproved user when document is missing`() = runWithServer({ request ->
        if (request.method == "GET") {
            FakeResponse(404)
        } else {
            FakeResponse(200, """{"name":"x/users/uid-1","fields":${request.body.substringAfter("\"fields\":")}""")
        }
    }) { server, repository ->
        val user = repository.getOrCreateUser(authUser).getOrThrow()

        assertEquals(AppUser(uid = "uid-1", email = "user@example.com", approved = false), user)
        val create = server.requests.last()
        assertEquals("POST", create.method)
        assertEquals(usersPath, create.path)
        assertEquals("documentId=uid-1", create.query)
        assertTrue(create.body.contains(""""approved":{"booleanValue":false}"""))
        assertTrue(create.body.contains(""""role":{"stringValue":"SPEAKING_ASSISTANT"}"""))
        assertTrue(create.body.contains(""""displayName":{"nullValue":null}"""))
    }

    @Test
    fun `getOrCreateUser returns failure for http errors, missing names and missing sign in`() {
        runWithServer({ FakeResponse(500) }) { _, repository ->
            assertIs<IOException>(repository.getOrCreateUser(authUser).exceptionOrNull())
        }
        runWithServer({ FakeResponse(200, """{"fields":{}}""") }) { _, repository ->
            assertIs<IOException>(repository.getOrCreateUser(authUser).exceptionOrNull())
        }
        runWithServer({ FakeResponse(200) }, token = null) { server, repository ->
            assertIs<IllegalStateException>(repository.getOrCreateUser(authUser).exceptionOrNull())
            assertTrue(server.requests.isEmpty())
        }
    }

    @Test
    fun `observeUser emits user and maps unknown or missing values to safe defaults`() = runWithServer({
        FakeResponse(200, """{"name":"x/users/uid-1","fields":{"role":{"stringValue":"SUPERUSER"}}}""")
    }) { _, repository ->
        assertEquals(
            AppUser(uid = "uid-1", approved = false, role = UserRole.NONE),
            repository.observeUser("uid-1").first(),
        )
    }

    @Test
    fun `observeUser emits null when user document does not exist`() = runWithServer({
        FakeResponse(404)
    }) { _, repository ->
        assertNull(repository.observeUser("uid-1").first())
    }

    @Test
    fun `desktop factory creates rest repository`() {
        assertIs<FirestoreRestUserRepository>(createUserRepository())
    }

    private fun runWithServer(
        responder: (RecordedRequest) -> FakeResponse,
        token: String? = "token-123",
        block: suspend (FakeFirestoreRestServer, FirestoreRestUserRepository) -> Unit,
    ) {
        FakeFirestoreRestServer(responder).use { server ->
            withFirestoreRestConfig(server) {
                runBlocking { block(server, FirestoreRestUserRepository(idTokenProvider = { token })) }
            }
        }
    }

    private fun userDocument(approved: Boolean, role: String): String = """{"name":"x/users/uid-1","fields":{
        "email":{"stringValue":"user@example.com"},
        "displayName":{"stringValue":"User"},
        "approved":{"booleanValue":$approved},
        "role":{"stringValue":"$role"}}}"""
}
