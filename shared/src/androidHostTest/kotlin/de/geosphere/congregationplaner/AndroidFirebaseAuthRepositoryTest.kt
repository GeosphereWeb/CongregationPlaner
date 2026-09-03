package de.geosphere.congregationplaner

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AndroidFirebaseAuthRepositoryTest {
    @Test
    fun `android auth repository fails safely when firebase app is missing`() {
        val previousContext = FirebaseAndroidContextHolder.context
        FirebaseAndroidContextHolder.context = null

        try {
            assertFailsWith<RuntimeException> {
                FirebaseAuthPlatformService()
            }
        } finally {
            FirebaseAndroidContextHolder.context = previousContext
        }
    }

    @Test
    fun `android sign in maps successful auth result into FirebaseUser`() = runBlocking {
        val auth = mockk<FirebaseAuth>()
        val resultTask = mockk<Task<AuthResult>>()
        val currentUser = mockk<FirebaseUser>()

        mockkStatic(FirebaseAuth::class)
        try {
            every { FirebaseAuth.getInstance() } returns auth
            every { auth.signInWithEmailAndPassword("user@example.com", "secret") } returns resultTask
            every { resultTask.addOnCompleteListener(any<OnCompleteListener<AuthResult>>()) } answers {
                val listener = firstArg<OnCompleteListener<AuthResult>>()
                listener.onComplete(resultTask)
                resultTask
            }
            every { resultTask.isSuccessful } returns true
            every { auth.currentUser } returns currentUser
            every { currentUser.uid } returns "android-user"
            every { currentUser.email } returns "user@example.com"
            every { currentUser.displayName } returns "Android User"

            val service = FirebaseAuthPlatformService()
            val user = service.signInWithEmailAndPassword("user@example.com", "secret")

            assertEquals("android-user", user?.uid)
            assertEquals("user@example.com", user?.email)
            assertEquals("Android User", user?.displayName)
            assertNull(user?.idToken)
            assertTrue(service.isSignedIn())
            assertEquals("android-user", service.currentUserId())
        } finally {
            unmockkStatic(FirebaseAuth::class)
        }
    }

    @Test
    fun `android create user succeeds when verification email succeeds`() = runBlocking {
        val auth = mockk<FirebaseAuth>()
        val createTask = mockk<Task<AuthResult>>()
        val verificationTask = mockk<Task<Void>>()
        val currentUser = mockk<FirebaseUser>()

        mockkStatic(FirebaseAuth::class)
        try {
            every { FirebaseAuth.getInstance() } returns auth
            every { auth.createUserWithEmailAndPassword("new@example.com", "secret") } returns createTask
            every { createTask.addOnCompleteListener(any<OnCompleteListener<AuthResult>>()) } answers {
                val listener = firstArg<OnCompleteListener<AuthResult>>()
                listener.onComplete(createTask)
                createTask
            }
            every { createTask.isSuccessful } returns true
            every { auth.currentUser } returns currentUser
            every { currentUser.uid } returns "new-android-user"
            every { currentUser.email } returns "new@example.com"
            every { currentUser.displayName } returns "New User"
            every { currentUser.sendEmailVerification() } returns verificationTask
            every { verificationTask.addOnCompleteListener(any<OnCompleteListener<Void>>()) } answers {
                val listener = firstArg<OnCompleteListener<Void>>()
                listener.onComplete(verificationTask)
                verificationTask
            }
            every { verificationTask.isSuccessful } returns true

            val service = FirebaseAuthPlatformService()
            val user = service.createUserWithEmailAndPassword("new@example.com", "secret")

            assertEquals("new-android-user", user?.uid)
            assertEquals("new@example.com", user?.email)
        } finally {
            unmockkStatic(FirebaseAuth::class)
        }
    }

    @Test
    fun `android create user fails when verification email fails`() = runBlocking {
        val auth = mockk<FirebaseAuth>()
        val createTask = mockk<Task<AuthResult>>()
        val verificationTask = mockk<Task<Void>>()
        val currentUser = mockk<FirebaseUser>()

        mockkStatic(FirebaseAuth::class)
        try {
            every { FirebaseAuth.getInstance() } returns auth
            every { auth.createUserWithEmailAndPassword("new@example.com", "secret") } returns createTask
            every { createTask.addOnCompleteListener(any<OnCompleteListener<AuthResult>>()) } answers {
                val listener = firstArg<OnCompleteListener<AuthResult>>()
                listener.onComplete(createTask)
                createTask
            }
            every { createTask.isSuccessful } returns true
            every { auth.currentUser } returns currentUser
            every { currentUser.uid } returns "new-android-user"
            every { currentUser.email } returns "new@example.com"
            every { currentUser.displayName } returns "New User"
            every { currentUser.sendEmailVerification() } returns verificationTask
            every { verificationTask.addOnCompleteListener(any<OnCompleteListener<Void>>()) } answers {
                val listener = firstArg<OnCompleteListener<Void>>()
                listener.onComplete(verificationTask)
                verificationTask
            }
            every { verificationTask.isSuccessful } returns false

            val service = FirebaseAuthPlatformService()
            val user = service.createUserWithEmailAndPassword("new@example.com", "secret")

            assertNull(user)
        } finally {
            unmockkStatic(FirebaseAuth::class)
        }
    }

    @Test
    fun `android platform factory creates a Firebase auth service`() {
        val auth = mockk<FirebaseAuth>()

        mockkStatic(FirebaseAuth::class)
        try {
            every { FirebaseAuth.getInstance() } returns auth

            val service = createFirebaseAuthPlatformService()

            assertTrue(service is FirebaseAuthPlatformService)
        } finally {
            unmockkStatic(FirebaseAuth::class)
        }
    }

    @Test
    fun `android sign in returns null when auth task fails`() = runBlocking {
        val auth = mockk<FirebaseAuth>()
        val resultTask = mockk<Task<AuthResult>>()

        mockkStatic(FirebaseAuth::class)
        try {
            every { FirebaseAuth.getInstance() } returns auth
            every { auth.signInWithEmailAndPassword("user@example.com", "secret") } returns resultTask
            every { resultTask.addOnCompleteListener(any<OnCompleteListener<AuthResult>>()) } answers {
                val listener = firstArg<OnCompleteListener<AuthResult>>()
                listener.onComplete(resultTask)
                resultTask
            }
            every { resultTask.isSuccessful } returns false

            val service = FirebaseAuthPlatformService()
            val user = service.signInWithEmailAndPassword("user@example.com", "secret")

            assertNull(user)
        } finally {
            unmockkStatic(FirebaseAuth::class)
        }
    }

    @Test
    fun `android create user returns null when no current user exists after signup`() = runBlocking {
        val auth = mockk<FirebaseAuth>()
        val createTask = mockk<Task<AuthResult>>()

        mockkStatic(FirebaseAuth::class)
        try {
            every { FirebaseAuth.getInstance() } returns auth
            every { auth.createUserWithEmailAndPassword("new@example.com", "secret") } returns createTask
            every { createTask.addOnCompleteListener(any<OnCompleteListener<AuthResult>>()) } answers {
                val listener = firstArg<OnCompleteListener<AuthResult>>()
                listener.onComplete(createTask)
                createTask
            }
            every { createTask.isSuccessful } returns true
            every { auth.currentUser } returns null

            val service = FirebaseAuthPlatformService()
            val user = service.createUserWithEmailAndPassword("new@example.com", "secret")

            assertNull(user)
        } finally {
            unmockkStatic(FirebaseAuth::class)
        }
    }

    @Test
    fun `android auth service tracks sign out and user state`() {
        val auth = mockk<FirebaseAuth>()
        val currentUser = mockk<FirebaseUser>()

        mockkStatic(FirebaseAuth::class)
        try {
            every { FirebaseAuth.getInstance() } returns auth
            every { auth.currentUser } returnsMany listOf(currentUser, currentUser, null)
            every { currentUser.uid } returns "uid-42"
            every { auth.signOut() } just Runs

            val service = FirebaseAuthPlatformService()

            assertTrue(service.isSignedIn())
            assertEquals("uid-42", service.currentUserId())

            runBlocking { service.signOut() }

            assertFalse(service.isSignedIn())
            assertNull(service.currentUserId())
        } finally {
            unmockkStatic(FirebaseAuth::class)
        }
    }
}
