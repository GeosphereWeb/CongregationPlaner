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

}
