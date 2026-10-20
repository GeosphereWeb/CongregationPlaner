package de.geosphere.congregationplaner.user.data

import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.FakeResponse
import de.geosphere.congregationplaner.testing.FakeFirestoreRestServer.RecordedRequest
import de.geosphere.congregationplaner.testing.withFirestoreRestConfig
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserPermission
import de.geosphere.congregationplaner.user.domain.model.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirestoreRestUserRepositoryTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin")
    private val membersPath = "${FakeFirestoreRestServer.DOCUMENTS_PATH}/${congregation.membersPath}"

    private val authUser = object : FirebaseUser {
        override val uid: String = "uid-1"
        override val email: String? = "user@example.com"
        override val displayName: String? = null
        override val idToken: String? = "token-123"
    }

    @Test
    fun `getOrCreateUser returns existing member document`() = runWithServer({
        FakeResponse(200, memberDocument(approved = true, role = "CONGREGATION_ADMIN"))
    }) { server, repository ->
        val user = repository.getOrCreateUser(authUser, congregation).getOrThrow()

        assertEquals(
            AppUser(
                uid = "uid-1",
                email = "user@example.com",
                displayName = "User",
                approved = true,
                role = UserRole.CONGREGATION_ADMIN,
                permissions = setOf(UserPermission.DUMMY),
                congregation = congregation,
            ),
            user,
        )
        val request = server.requests.single()
        assertEquals("GET", request.method)
        assertEquals("$membersPath/uid-1", request.path)
        assertEquals("Bearer token-123", request.authorization)
    }

    @Test
    fun `getOrCreateUser creates unapproved member with default permission when document is missing`() =
        runWithServer({ request ->
            if (request.method == "GET") {
                FakeResponse(404)
            } else {
                val fields = request.body.substringAfter("\"fields\":")
                FakeResponse(200, """{"name":"x/members/uid-1","fields":$fields""")
            }
        }) { server, repository ->
            val user = repository.getOrCreateUser(authUser, congregation).getOrThrow()

            assertEquals(
                AppUser(
                    uid = "uid-1",
                    email = "user@example.com",
                    approved = false,
                    role = UserRole.MEMBER,
                    permissions = setOf(UserPermission.DUMMY),
                    congregation = congregation,
                ),
                user,
            )
            val create = server.requests.last()
            assertEquals("POST", create.method)
            assertEquals(membersPath, create.path)
            assertEquals("documentId=uid-1", create.query)
            assertTrue(create.body.contains(""""approved":{"booleanValue":false}"""))
            assertTrue(create.body.contains(""""role":{"stringValue":"MEMBER"}"""))
            assertTrue(create.body.contains(""""permissions":{"arrayValue":{"values":[{"stringValue":"DUMMY"}]}}"""))
            assertTrue(create.body.contains(""""displayName":{"nullValue":null}"""))
        }

    @Test
    fun `getOrCreateUser returns failure for http errors, missing names and missing sign in`() {
        runWithServer({ FakeResponse(500) }) { _, repository ->
            assertIs<IOException>(repository.getOrCreateUser(authUser, congregation).exceptionOrNull())
        }
        runWithServer({ FakeResponse(200, """{"fields":{}}""") }) { _, repository ->
            assertIs<IOException>(repository.getOrCreateUser(authUser, congregation).exceptionOrNull())
        }
        runWithServer({ FakeResponse(200) }, token = null) { server, repository ->
            assertIs<IllegalStateException>(repository.getOrCreateUser(authUser, congregation).exceptionOrNull())
            assertTrue(server.requests.isEmpty())
        }
    }

    @Test
    fun `observeUser maps unknown or missing values to safe defaults`() = runWithServer({
        FakeResponse(200, """{"name":"x/members/uid-1","fields":{"role":{"stringValue":"ADMIN"}}}""")
    }) { _, repository ->
        assertEquals(
            AppUser(uid = "uid-1", approved = false, role = UserRole.NONE, congregation = congregation),
            repository.observeUser(congregation, "uid-1").first(),
        )
    }

    @Test
    fun `observeUser emits null when member document does not exist`() = runWithServer({
        FakeResponse(404)
    }) { _, repository ->
        assertNull(repository.observeUser(congregation, "uid-1").first())
    }

    @Test
    fun `isGlobalAdmin checks the admins collection`() {
        runWithServer({ FakeResponse(200, """{"name":"x/admins/uid-1"}""") }) { server, repository ->
            assertTrue(repository.isGlobalAdmin("uid-1").getOrThrow())
            assertEquals("${FakeFirestoreRestServer.DOCUMENTS_PATH}/admins/uid-1", server.requests.single().path)
        }
        runWithServer({ FakeResponse(404) }) { _, repository ->
            assertFalse(repository.isGlobalAdmin("uid-1").getOrThrow())
        }
        runWithServer({ FakeResponse(403) }) { _, repository ->
            assertIs<IOException>(repository.isGlobalAdmin("uid-1").exceptionOrNull())
        }
    }

    @Test
    fun `observeMembers lists the members of the congregation`() = runWithServer({
        FakeResponse(200, """{"documents":[${memberDocument(approved = false, role = "MEMBER")}]}""")
    }) { server, repository ->
        val members = repository.observeMembers(congregation).first()

        assertEquals(listOf("uid-1"), members.map { it.uid })
        assertEquals(membersPath, server.requests.single().path)
    }

    @Test
    fun `observeMembers emits empty list for empty collections`() = runWithServer({
        FakeResponse(200, "{}")
    }) { _, repository ->
        assertTrue(repository.observeMembers(congregation).first().isEmpty())
    }

    @Test
    fun `updateMember patches only access fields of an existing member`() = runWithServer({
        FakeResponse(200, "{}")
    }) { server, repository ->
        val member = AppUser(uid = "uid-1", email = "ignored", approved = true, role = UserRole.CONGREGATION_ADMIN)

        assertEquals(member, repository.updateMember(congregation, member).getOrThrow())

        val request = server.requests.single()
        assertEquals("PATCH", request.method)
        assertEquals("$membersPath/uid-1", request.path)
        assertEquals(
            "updateMask.fieldPaths=approved&updateMask.fieldPaths=role&updateMask.fieldPaths=permissions" +
                "&currentDocument.exists=true",
            request.query,
        )
        assertFalse(request.body.contains("email"))
        assertTrue(request.body.contains(""""permissions":{"arrayValue":{"values":[]}}"""))
    }

    @Test
    fun `removeMember deletes the member document and reports failures`() {
        runWithServer({ FakeResponse(200, "{}") }) { server, repository ->
            repository.removeMember(congregation, "uid-1").getOrThrow()
            assertEquals("DELETE", server.requests.single().method)
            assertEquals("$membersPath/uid-1", server.requests.single().path)
        }
        runWithServer({ FakeResponse(403) }) { _, repository ->
            assertIs<IOException>(repository.removeMember(congregation, "uid-1").exceptionOrNull())
        }
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

    private fun memberDocument(approved: Boolean, role: String): String = """{"name":"x/members/uid-1","fields":{
        "email":{"stringValue":"user@example.com"},
        "displayName":{"stringValue":"User"},
        "approved":{"booleanValue":$approved},
        "role":{"stringValue":"$role"},
        "permissions":{"arrayValue":{"values":[{"stringValue":"DUMMY"},{"stringValue":"UNKNOWN"}]}}}}"""
}
