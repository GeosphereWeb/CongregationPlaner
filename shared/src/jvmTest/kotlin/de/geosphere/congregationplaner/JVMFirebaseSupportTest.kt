package de.geosphere.congregationplaner

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JVMFirebaseSupportTest {
    @Test
    fun `desktop platform support is ready when api key is configured`() {
        val previousValue = System.getProperty("FIREBASE_WEB_API_KEY")
        val previousDotValue = System.getProperty("firebase.webApiKey")

        try {
            System.setProperty("FIREBASE_WEB_API_KEY", "desktop-test-key")
            System.clearProperty("firebase.webApiKey")

            val support = FirebasePlatformSupport()
            support.initialize()

            assertTrue(support.isReady())
        } finally {
            restoreSystemProperty("FIREBASE_WEB_API_KEY", previousValue)
            restoreSystemProperty("firebase.webApiKey", previousDotValue)
        }
    }

    @Test
    fun `desktop platform support stays unready when api key is missing`() {
        val previousValue = System.getProperty("FIREBASE_WEB_API_KEY")
        val previousDotValue = System.getProperty("firebase.webApiKey")

        try {
            System.clearProperty("FIREBASE_WEB_API_KEY")
            System.clearProperty("firebase.webApiKey")

            val support = FirebasePlatformSupport()
            support.initialize()

            assertFalse(support.isReady())
        } finally {
            restoreSystemProperty("FIREBASE_WEB_API_KEY", previousValue)
            restoreSystemProperty("firebase.webApiKey", previousDotValue)
        }
    }

    @Test
    fun `firebase auth manager delegates to injected platform service`() = runBlocking {
        val previousFactory = firebaseAuthPlatformServiceFactory
        val expectedUser = object : FirebaseUser {
            override val uid: String = "manager-user"
            override val email: String? = "manager@example.com"
            override val displayName: String? = "Manager User"
            override val idToken: String? = "manager-token"
        }
        val repository = object : FirebaseAuthRepository {
            override suspend fun signInWithEmailAndPassword(email: String, password: String): FirebaseUser? = expectedUser
            override suspend fun createUserWithEmailAndPassword(email: String, password: String): FirebaseUser? = expectedUser
            override suspend fun signOut() = Unit
            override fun currentUserId(): String? = "manager-user"
            override fun isSignedIn(): Boolean = true
        }

        try {
            firebaseAuthPlatformServiceFactory = { repository }
            val signedIn = FirebaseAuthManager.signInWithEmailAndPassword("mail@example.com", "pw")
            val created = FirebaseAuthManager.createUserWithEmailAndPassword("mail@example.com", "pw")

            assertEquals("manager-user", signedIn?.uid)
            assertEquals("manager-user", created?.uid)
            assertEquals("manager-user", FirebaseAuthManager.currentUserId())
            assertTrue(FirebaseAuthManager.isSignedIn())

            FirebaseAuthManager.signOut()
            assertTrue(FirebaseAuthManager.isSignedIn())
        } finally {
            firebaseAuthPlatformServiceFactory = previousFactory
        }
    }

    @Test
    fun `desktop auth service starts unauthenticated`() = runBlocking {
        val service = FirebaseAuthPlatformService()
        assertNull(service.currentUserId())
        assertFalse(service.isSignedIn())

        service.signOut()

        assertNull(service.currentUserId())
        assertFalse(service.isSignedIn())
    }

    private fun restoreSystemProperty(key: String, previousValue: String?) {
        if (previousValue == null) {
            System.clearProperty(key)
        } else {
            System.setProperty(key, previousValue)
        }
    }
}
