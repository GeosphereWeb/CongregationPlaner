package de.geosphere.congregationplaner

import com.google.firebase.FirebaseApp
import android.app.Activity
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlin.test.assertEquals
import kotlin.test.Test
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
    fun `android platform support does not initialize an existing firebase app`() {
        val originalContext = FirebaseAndroidContextHolder.context
        val app = mockk<FirebaseApp>()

        try {
            FirebaseAndroidContextHolder.context = mockkContext()
            mockkStatic(FirebaseApp::class)
            every { FirebaseApp.getApps(any()) } returns listOf(app)

            FirebasePlatformSupport().initialize()

            assertTrue(FirebasePlatformSupport().isReady())
        } finally {
            FirebaseAndroidContextHolder.context = originalContext
            unmockkStatic(FirebaseApp::class)
        }
    }

    @Test
    fun `android context holder stores configured authentication settings`() {
        val previousContext = FirebaseAndroidContextHolder.context
        val previousActivity = FirebaseAndroidContextHolder.activity
        val previousUseEmulator = FirebaseAndroidContextHolder.useAuthEmulator
        val previousEmulatorHost = FirebaseAndroidContextHolder.authEmulatorHost
        val context = mockkContext()
        val activity = mockk<Activity>(relaxed = true)

        try {
            FirebaseAndroidContextHolder.configure(
                context = context,
                activity = activity,
                useAuthEmulator = true,
                authEmulatorHost = "localhost",
            )

            assertEquals(context, FirebaseAndroidContextHolder.context)
            assertEquals(activity, FirebaseAndroidContextHolder.activity)
            assertTrue(FirebaseAndroidContextHolder.useAuthEmulator)
            assertEquals("localhost", FirebaseAndroidContextHolder.authEmulatorHost)
        } finally {
            FirebaseAndroidContextHolder.configure(
                context = previousContext ?: mockkContext(),
                activity = previousActivity ?: mockk(relaxed = true),
                useAuthEmulator = previousUseEmulator,
                authEmulatorHost = previousEmulatorHost,
            )
            FirebaseAndroidContextHolder.context = previousContext
            FirebaseAndroidContextHolder.activity = previousActivity
        }
    }

    private fun mockkContext(): android.content.Context = io.mockk.mockk(relaxed = true)
}
