package de.geosphere.congregationplaner

import com.google.firebase.FirebaseApp
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
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

    private fun mockkContext(): android.content.Context = io.mockk.mockk(relaxed = true)
}
