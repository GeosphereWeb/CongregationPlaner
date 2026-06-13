package de.geosphere.congregationplaner

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsPlatformTest {
    @Test
    fun testJsPlatformActualReturnsValidBrowserName() {
        val platform = getPlatform()
        assertTrue(platform.name.isNotBlank(), "Js getPlatform() should return a valid browser name")
    }

    @Test
    fun testJsPlatformActualIsDesktopIsFalse() {
        assertFalse(getPlatform().isDesktop, "Js platform should not be desktop")
    }
}
