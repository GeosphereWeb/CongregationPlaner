package de.geosphere.congregationplaner.auth

import android.app.Activity
import android.content.Context
import com.google.firebase.FirebaseApp
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidFirebaseSupportTest {
    @Test
    fun `android firebase support is not ready without context`() {
        FirebaseAndroidContextHolder.context = null

        FirebaseSupport.initialize()

        assertFalse(FirebaseSupport.isReady())
    }

    @Test
    fun `android platform support stays safe without context`() {
        FirebaseAndroidContextHolder.context = null
        val platformSupport = FirebasePlatformSupport()

        platformSupport.initialize()

        assertFalse(platformSupport.isReady())
    }

    @Test
    fun `android platform support initializes firebase app when context is present`() {
        val originalContext = FirebaseAndroidContextHolder.context
        val app = io.mockk.mockk<FirebaseApp>()
        var initialized = false

        try {
            FirebaseAndroidContextHolder.context = mockkContext()
            mockkStatic(FirebaseApp::class)
            every { FirebaseApp.getApps(any()) } answers {
                if (initialized) listOf(app) else emptyList()
            }
            every { FirebaseApp.initializeApp(any()) } answers {
                initialized = true
                app
            }

            val support = FirebasePlatformSupport()
            support.initialize()

            assertTrue(support.isReady())
        } finally {
            FirebaseAndroidContextHolder.context = originalContext
            unmockkStatic(FirebaseApp::class)
        }
    }

    @Test
    fun `android platform support skips firebase initialization when app already exists`() {
        val previousContext = FirebaseAndroidContextHolder.context
        val app = io.mockk.mockk<FirebaseApp>()

        try {
            FirebaseAndroidContextHolder.context = mockkContext()
            mockkStatic(FirebaseApp::class)
            every { FirebaseApp.getApps(any()) } returns listOf(app)

            FirebasePlatformSupport().initialize()

            assertTrue(FirebasePlatformSupport().isReady())
        } finally {
            FirebaseAndroidContextHolder.context = previousContext
            unmockkStatic(FirebaseApp::class)
        }
    }

    @Test
    fun `android context holder configures emulator settings and defaults host`() {
        val previousContext = FirebaseAndroidContextHolder.context
        val previousActivity = FirebaseAndroidContextHolder.activity
        val previousUseEmulator = FirebaseAndroidContextHolder.useAuthEmulator
        val previousHost = FirebaseAndroidContextHolder.authEmulatorHost
        val context = mockk<Context>(relaxed = true)
        val activity = mockk<Activity>(relaxed = true)

        try {
            FirebaseAndroidContextHolder.configure(context, activity, true, "auth.local")

            assertTrue(FirebaseAndroidContextHolder.context === context)
            assertTrue(FirebaseAndroidContextHolder.activity === activity)
            assertTrue(FirebaseAndroidContextHolder.useAuthEmulator)
            assertEquals("auth.local", FirebaseAndroidContextHolder.authEmulatorHost)

            FirebaseAndroidContextHolder.configure(context, activity, false)
            assertFalse(FirebaseAndroidContextHolder.useAuthEmulator)
            assertEquals("10.0.2.2", FirebaseAndroidContextHolder.authEmulatorHost)
        } finally {
            FirebaseAndroidContextHolder.configure(
                previousContext ?: mockk(relaxed = true),
                previousActivity ?: mockk(relaxed = true),
                previousUseEmulator,
                previousHost,
            )
            FirebaseAndroidContextHolder.context = previousContext
            FirebaseAndroidContextHolder.activity = previousActivity
        }
    }

    private fun mockkContext(): android.content.Context = io.mockk.mockk(relaxed = true)
}
