package de.geosphere.congregationplaner

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FirebaseRepositoryContractTest {
    @Test
    fun `fake user exposes expected values`() {
        val user = object : FirebaseUser {
            override val uid: String = "uid-123"
            override val email: String? = "user@example.com"
            override val displayName: String? = "User Name"
            override val idToken: String? = "token-123"
        }

        assertEquals("uid-123", user.uid)
        assertEquals("user@example.com", user.email)
        assertEquals("User Name", user.displayName)
        assertEquals("token-123", user.idToken)
    }

    @Test
    fun `fake repository tracks auth state`() = runBlocking {
        val repository = object : FirebaseAuthRepository {
            private var signedIn = true

            override suspend fun signInWithEmailAndPassword(email: String, password: String): FirebaseUser? = null
            override suspend fun createUserWithEmailAndPassword(email: String, password: String): FirebaseUser? = null
            override suspend fun signOut() {
                signedIn = false
            }

            override fun currentUserId(): String? = if (signedIn) "user-42" else null
            override fun isSignedIn(): Boolean = signedIn
        }

        assertEquals("user-42", repository.currentUserId())
        assertTrue(repository.isSignedIn())

        repository.signOut()

        assertFalse(repository.isSignedIn())
        assertEquals(null, repository.currentUserId())
    }
}
