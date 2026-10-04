package de.geosphere.congregationplaner

import android.app.Activity
import android.content.res.Resources
import android.util.Log
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
        val authResult = mockk<AuthResult>()
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
            every { resultTask.result } returns authResult
            every { authResult.user } returns currentUser
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
    fun `android sign in falls back to current firebase user when auth result has no user`() = runBlocking {
        val auth = mockk<FirebaseAuth>()
        val resultTask = mockk<Task<AuthResult>>()
        val authResult = mockk<AuthResult>()
        val currentUser = mockk<FirebaseUser>()

        mockkStatic(FirebaseAuth::class)
        try {
            every { FirebaseAuth.getInstance() } returns auth
            every { auth.signInWithEmailAndPassword("user@example.com", "secret") } returns resultTask
            every { resultTask.addOnCompleteListener(any<OnCompleteListener<AuthResult>>()) } answers {
                firstArg<OnCompleteListener<AuthResult>>().onComplete(resultTask)
                resultTask
            }
            every { resultTask.isSuccessful } returns true
            every { resultTask.result } returns authResult
            every { authResult.user } returns null
            every { auth.currentUser } returns currentUser
            every { currentUser.uid } returns "current-user"
            every { currentUser.email } returns "user@example.com"
            every { currentUser.displayName } returns "Current User"

            val user = FirebaseAuthPlatformService()
                .signInWithEmailAndPassword("user@example.com", "secret")

            assertEquals("current-user", user?.uid)
            assertEquals("user@example.com", user?.email)
            assertEquals("Current User", user?.displayName)
        } finally {
            unmockkStatic(FirebaseAuth::class)
        }
    }

    @Test
    fun `android create user succeeds when verification email succeeds`() = runBlocking {
        val auth = mockk<FirebaseAuth>()
        val createTask = mockk<Task<AuthResult>>()
        val verificationTask = mockk<Task<Void>>()
        val authResult = mockk<AuthResult>()
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
            every { createTask.result } returns authResult
            every { authResult.user } returns currentUser
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
    fun `android create user falls back to current firebase user when auth result has no user`() = runBlocking {
        val auth = mockk<FirebaseAuth>()
        val createTask = mockk<Task<AuthResult>>()
        val verificationTask = mockk<Task<Void>>()
        val authResult = mockk<AuthResult>()
        val currentUser = mockk<FirebaseUser>()

        mockkStatic(FirebaseAuth::class)
        try {
            every { FirebaseAuth.getInstance() } returns auth
            every { auth.createUserWithEmailAndPassword("new@example.com", "secret") } returns createTask
            every { createTask.addOnCompleteListener(any<OnCompleteListener<AuthResult>>()) } answers {
                firstArg<OnCompleteListener<AuthResult>>().onComplete(createTask)
                createTask
            }
            every { createTask.isSuccessful } returns true
            every { createTask.result } returns authResult
            every { authResult.user } returns null
            every { auth.currentUser } returns currentUser
            every { currentUser.uid } returns "new-current-user"
            every { currentUser.email } returns "new@example.com"
            every { currentUser.displayName } returns "New User"
            every { currentUser.sendEmailVerification() } returns verificationTask
            every { verificationTask.addOnCompleteListener(any<OnCompleteListener<Void>>()) } answers {
                firstArg<OnCompleteListener<Void>>().onComplete(verificationTask)
                verificationTask
            }
            every { verificationTask.isSuccessful } returns true

            val user = FirebaseAuthPlatformService()
                .createUserWithEmailAndPassword("new@example.com", "secret")

            assertEquals("new-current-user", user?.uid)
            assertEquals("new@example.com", user?.email)
        } finally {
            unmockkStatic(FirebaseAuth::class)
        }
    }

    @Test
    fun `android create user returns null when account creation task fails`() = runBlocking {
        val auth = mockk<FirebaseAuth>()
        val createTask = mockk<Task<AuthResult>>()

        mockkStatic(FirebaseAuth::class)
        mockkStatic(Log::class)
        try {
            every { FirebaseAuth.getInstance() } returns auth
            every { auth.createUserWithEmailAndPassword("new@example.com", "secret") } returns createTask
            every { createTask.addOnCompleteListener(any<OnCompleteListener<AuthResult>>()) } answers {
                firstArg<OnCompleteListener<AuthResult>>().onComplete(createTask)
                createTask
            }
            every { createTask.isSuccessful } returns false
            every { createTask.exception } returns null
            every { Log.e(any(), any(), any<Throwable>()) } returns 0

            val user = FirebaseAuthPlatformService()
                .createUserWithEmailAndPassword("new@example.com", "secret")

            assertNull(user)
        } finally {
            unmockkStatic(FirebaseAuth::class)
            unmockkStatic(Log::class)
        }
    }

    @Test
    fun `android create user fails when verification email fails`() = runBlocking {
        val auth = mockk<FirebaseAuth>()
        val createTask = mockk<Task<AuthResult>>()
        val verificationTask = mockk<Task<Void>>()
        val authResult = mockk<AuthResult>()
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
            every { createTask.result } returns authResult
            every { authResult.user } returns currentUser
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
            every { verificationTask.exception } returns null
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any<Throwable>()) } returns 0

            val service = FirebaseAuthPlatformService()
            val user = service.createUserWithEmailAndPassword("new@example.com", "secret")

            assertNull(user)
        } finally {
            unmockkStatic(FirebaseAuth::class)
            unmockkStatic(Log::class)
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
    fun `android google sign in is unavailable when activity is missing`() = runBlocking {
        val previousActivity = FirebaseAndroidContextHolder.activity
        val auth = mockk<FirebaseAuth>()

        try {
            FirebaseAndroidContextHolder.activity = null
            mockkStatic(FirebaseAuth::class)
            every { FirebaseAuth.getInstance() } returns auth

            val service = FirebaseAuthPlatformService()

            assertFalse(service.isGoogleSignInAvailable())
            assertNull(service.signInWithGoogle())
        } finally {
            FirebaseAndroidContextHolder.activity = previousActivity
            unmockkStatic(FirebaseAuth::class)
        }
    }

    @Test
    fun `android google sign in returns null when client id resource is missing`() = runBlocking {
        val previousActivity = FirebaseAndroidContextHolder.activity
        val auth = mockk<FirebaseAuth>()
        val activity = mockk<Activity>()
        val resources = mockk<Resources>()

        mockkStatic(FirebaseAuth::class)
        mockkStatic(Log::class)
        try {
            FirebaseAndroidContextHolder.activity = activity
            every { FirebaseAuth.getInstance() } returns auth
            every { activity.resources } returns resources
            every { activity.packageName } returns "test.package"
            every { resources.getIdentifier("default_web_client_id", "string", "test.package") } returns 0
            every { Log.e(any(), any()) } returns 0

            val service = FirebaseAuthPlatformService()

            assertTrue(service.isGoogleSignInAvailable())
            assertNull(service.signInWithGoogle())
        } finally {
            FirebaseAndroidContextHolder.activity = previousActivity
            unmockkStatic(FirebaseAuth::class)
            unmockkStatic(Log::class)
        }
    }

    @Test
    fun `android create user skips email verification when auth emulator is enabled`() = runBlocking {
        val previousContext = FirebaseAndroidContextHolder.context
        val previousActivity = FirebaseAndroidContextHolder.activity
        val previousUseEmulator = FirebaseAndroidContextHolder.useAuthEmulator
        val previousEmulatorHost = FirebaseAndroidContextHolder.authEmulatorHost
        val context = mockk<android.content.Context>(relaxed = true)
        val activity = mockk<Activity>(relaxed = true)
        val auth = mockk<FirebaseAuth>()
        val createTask = mockk<Task<AuthResult>>()
        val authResult = mockk<AuthResult>()
        val currentUser = mockk<FirebaseUser>()

        mockkStatic(FirebaseAuth::class)
        try {
            FirebaseAndroidContextHolder.configure(context, activity, useAuthEmulator = true)
            every { FirebaseAuth.getInstance() } returns auth
            every { auth.useEmulator(any(), any()) } just Runs
            every { auth.createUserWithEmailAndPassword("new@example.com", "secret") } returns createTask
            every { createTask.addOnCompleteListener(any<OnCompleteListener<AuthResult>>()) } answers {
                firstArg<OnCompleteListener<AuthResult>>().onComplete(createTask)
                createTask
            }
            every { createTask.isSuccessful } returns true
            every { createTask.result } returns authResult
            every { authResult.user } returns currentUser
            every { auth.currentUser } returns currentUser
            every { currentUser.uid } returns "emulator-user"
            every { currentUser.email } returns "new@example.com"
            every { currentUser.displayName } returns null

            val user = FirebaseAuthPlatformService().createUserWithEmailAndPassword("new@example.com", "secret")

            assertEquals("emulator-user", user?.uid)
        } finally {
            FirebaseAndroidContextHolder.configure(
                context = previousContext ?: context,
                activity = previousActivity ?: activity,
                useAuthEmulator = previousUseEmulator,
                authEmulatorHost = previousEmulatorHost,
            )
            FirebaseAndroidContextHolder.context = previousContext
            FirebaseAndroidContextHolder.activity = previousActivity
            unmockkStatic(FirebaseAuth::class)
        }
    }

    @Test
    fun `android sign in returns null when auth task fails`() = runBlocking {
        val auth = mockk<FirebaseAuth>()
        val resultTask = mockk<Task<AuthResult>>()

        mockkStatic(FirebaseAuth::class)
        mockkStatic(Log::class)
        try {
            every { FirebaseAuth.getInstance() } returns auth
            every { auth.signInWithEmailAndPassword("user@example.com", "secret") } returns resultTask
            every { resultTask.addOnCompleteListener(any<OnCompleteListener<AuthResult>>()) } answers {
                val listener = firstArg<OnCompleteListener<AuthResult>>()
                listener.onComplete(resultTask)
                resultTask
            }
            every { resultTask.isSuccessful } returns false
            every { resultTask.exception } returns null
            every { Log.e(any(), any(), any<Throwable>()) } returns 0

            val service = FirebaseAuthPlatformService()
            val user = service.signInWithEmailAndPassword("user@example.com", "secret")

            assertNull(user)
        } finally {
            unmockkStatic(FirebaseAuth::class)
            unmockkStatic(Log::class)
        }
    }

    @Test
    fun `android create user returns null when no current user exists after signup`() = runBlocking {
        val auth = mockk<FirebaseAuth>()
        val createTask = mockk<Task<AuthResult>>()
        val authResult = mockk<AuthResult>()

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
            every { createTask.result } returns authResult
            every { authResult.user } returns null
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
