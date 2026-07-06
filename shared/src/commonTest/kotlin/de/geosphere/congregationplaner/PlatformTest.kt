package de.geosphere.congregationplaner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformTest {
    @Test
    fun testGetPlatformReturnsValidName() {
        val platform = getPlatform()
        assertTrue(platform.name.isNotBlank(), "Platform name should not be blank")
    }

    @Test
    fun testHostPlatformProvidesCurrentPlatform() {
        assertEquals(getPlatform().name, HostPlatform.current.name, "HostPlatform.current should match getPlatform()")
    }

    @Test
    fun testHostPlatformIsDesktopMatchesPlatformProperty() {
        assertEquals(
            HostPlatform.current.isDesktop,
            HostPlatform.isDesktop,
            "HostPlatform.isDesktop should match current platform's isDesktop",
        )
    }

    @Test
    fun testDetectBrowserNameKnownBrowser() {
        assertEquals(
            "Chrome/120.0.0.0",
            detectBrowserName(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            ),
        )
    }

    @Test
    fun testDetectBrowserNameUnknown() {
        assertEquals("Unknown", detectBrowserName("SomeCustomAgent/1.0"))
    }

    @Test
    fun testJsPlatformReturnsValidBrowserName() {
        val platform = JsPlatform("Mozilla/5.0 Chrome/120.0 Safari/537.36")
        assertTrue(platform.name.isNotBlank(), "JsPlatform name should not be blank")
        assertEquals("Chrome/120.0", platform.name)
    }

    @Test
    fun testJsPlatformIsDesktopIsFalse() {
        assertFalse(JsPlatform("Mozilla/5.0 Chrome/120.0").isDesktop, "JsPlatform.isDesktop should be false")
    }
}
