package de.geosphere.congregationplaner

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JVMFirebaseAuthRepositoryTest {
    @Test
    fun `desktop sign in maps successful auth response into FirebaseUser`() = runBlocking {
        val previousKey = System.getProperty("FIREBASE_WEB_API_KEY")
        System.setProperty("FIREBASE_WEB_API_KEY", "desktop-test-key")

        try {
            val connection = mockConnection(
                200,
                """{"localId":"svc-user","email":"svc@example.com","displayName":"Svc User","idToken":"svc-token"}""",
            )

            val service = FirebaseAuthPlatformService { _ -> connection }
            val user = service.signInWithEmailAndPassword("svc@example.com", "secret")

            assertEquals("svc-user", user?.uid)
            assertEquals("svc@example.com", user?.email)
            assertEquals("Svc User", user?.displayName)
            assertEquals("svc-token", user?.idToken)
            assertTrue(service.isSignedIn())
            assertEquals("svc-user", service.currentUserId())
        } finally {
            restoreSystemProperty("FIREBASE_WEB_API_KEY", previousKey)
        }
    }

    @Test
    fun `desktop platform factory creates a Firebase auth service`() {
        val previousKey = System.getProperty("FIREBASE_WEB_API_KEY")
        System.setProperty("FIREBASE_WEB_API_KEY", "desktop-test-key")

        try {
            val service = createFirebaseAuthPlatformService()
            assertTrue(service is FirebaseAuthPlatformService)
        } finally {
            restoreSystemProperty("FIREBASE_WEB_API_KEY", previousKey)
        }
    }

    @Test
    fun `desktop sign in returns null when api key is missing`() = runBlocking {
        val previousKey = System.getProperty("FIREBASE_WEB_API_KEY")
        System.clearProperty("FIREBASE_WEB_API_KEY")

        try {
            val service = FirebaseAuthPlatformService { _ -> error("should not be called") }
            assertNull(service.signInWithEmailAndPassword("svc@example.com", "secret"))
        } finally {
            restoreSystemProperty("FIREBASE_WEB_API_KEY", previousKey)
        }
    }

    @Test
    fun `desktop sign in returns null when server rejects request`() = runBlocking {
        val previousKey = System.getProperty("FIREBASE_WEB_API_KEY")
        System.setProperty("FIREBASE_WEB_API_KEY", "desktop-test-key")

        try {
            val connection = mockConnection(
                400,
                """{"error":{"message":"bad request"}}""",
            )

            val service = FirebaseAuthPlatformService { _ -> connection }
            assertNull(service.signInWithEmailAndPassword("svc@example.com", "secret"))
        } finally {
            restoreSystemProperty("FIREBASE_WEB_API_KEY", previousKey)
        }
    }

    @Test
    fun `desktop create user returns user without verification when no id token is returned`() = runBlocking {
        val previousKey = System.getProperty("FIREBASE_WEB_API_KEY")
        System.setProperty("FIREBASE_WEB_API_KEY", "desktop-test-key")

        try {
            val signUpConnection = mockConnection(
                200,
                """{"localId":"new-user","email":"new@example.com","displayName":"New User"}""",
            )

            val service = FirebaseAuthPlatformService { _ -> signUpConnection }
            val user = service.createUserWithEmailAndPassword("new@example.com", "secret")

            assertEquals("new-user", user?.uid)
            assertEquals("new@example.com", user?.email)
            assertNull(user?.idToken)
            assertTrue(service.isSignedIn())
        } finally {
            restoreSystemProperty("FIREBASE_WEB_API_KEY", previousKey)
        }
    }

    @Test
    fun `desktop create user returns null when sign up response is missing a user`() = runBlocking {
        val previousKey = System.getProperty("FIREBASE_WEB_API_KEY")
        System.setProperty("FIREBASE_WEB_API_KEY", "desktop-test-key")

        try {
            val failedConnection = mockConnection(
                400,
                """{"error":{"message":"signup failed"}}""",
            )

            val service = FirebaseAuthPlatformService { _ -> failedConnection }
            assertNull(service.createUserWithEmailAndPassword("new@example.com", "secret"))
        } finally {
            restoreSystemProperty("FIREBASE_WEB_API_KEY", previousKey)
        }
    }

    @Test
    fun `desktop create user succeeds when verification email succeeds`() = runBlocking {
        val previousKey = System.getProperty("FIREBASE_WEB_API_KEY")
        System.setProperty("FIREBASE_WEB_API_KEY", "desktop-test-key")

        try {
            val signUpConnection = mockConnection(
                200,
                """{"localId":"new-user","email":"new@example.com","displayName":"New User","idToken":"new-token"}""",
            )
            val verifyConnection = mockConnection(
                200,
                """{"kind":"identitytoolkit#GetOobConfirmationCodeResponse"}""",
            )

            var callCount = 0
            val service = FirebaseAuthPlatformService { _ ->
                callCount += 1
                if (callCount == 1) signUpConnection else verifyConnection
            }
            val user = service.createUserWithEmailAndPassword("new@example.com", "secret")

            assertEquals("new-user", user?.uid)
            assertEquals("new-user", service.currentUserId())
        } finally {
            restoreSystemProperty("FIREBASE_WEB_API_KEY", previousKey)
        }
    }

    @Test
    fun `desktop create user fails when verification email fails`() = runBlocking {
        val previousKey = System.getProperty("FIREBASE_WEB_API_KEY")
        System.setProperty("FIREBASE_WEB_API_KEY", "desktop-test-key")

        try {
            val signUpConnection = mockConnection(
                200,
                """{"localId":"new-user","email":"new@example.com","displayName":"New User","idToken":"new-token"}""",
            )
            val failedVerificationConnection = mockConnection(
                500,
                """{"error":{"message":"verification failed"}}""",
            )

            var callCount = 0
            val service = FirebaseAuthPlatformService { _ ->
                callCount += 1
                if (callCount == 1) signUpConnection else failedVerificationConnection
            }
            assertNull(service.createUserWithEmailAndPassword("new@example.com", "secret"))
        } finally {
            restoreSystemProperty("FIREBASE_WEB_API_KEY", previousKey)
        }
    }

    private fun mockConnection(status: Int, body: String?): HttpURLConnection {
        val connection = mockk<HttpURLConnection>()
        every { connection.responseCode } returns status
        every { connection.outputStream } returns ByteArrayOutputStream()
        every { connection.setRequestMethod(any()) } just Runs
        every { connection.setRequestProperty(any(), any()) } just Runs
        every { connection.doOutput = any() } just Runs
        every { connection.requestMethod = any() } just Runs
        every { connection.doOutput } returns true
        every { connection.requestMethod } returns "POST"

        val responseBytes = (body ?: "").toByteArray(StandardCharsets.UTF_8)
        every { connection.inputStream } returns ByteArrayInputStream(responseBytes)
        every { connection.errorStream } returns if (status in 200..299) null else ByteArrayInputStream("error".toByteArray(StandardCharsets.UTF_8))
        return connection
    }

    private fun restoreSystemProperty(key: String, previousValue: String?) {
        if (previousValue == null) {
            System.clearProperty(key)
        } else {
            System.setProperty(key, previousValue)
        }
    }
}
