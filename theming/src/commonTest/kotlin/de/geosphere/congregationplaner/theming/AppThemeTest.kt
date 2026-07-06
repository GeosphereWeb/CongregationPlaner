package de.geosphere.congregationplaner.theming

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AppThemeTest {

    @Test
    fun testLightColorScheme() {
        val scheme = AppTheme.colorScheme(isDark = false)
        assertNotNull(scheme.primary)
        assertNotNull(scheme.secondary)
        assertEquals(scheme.primary.toString(), AppColors.Primary.toString())
    }

    @Test
    fun testDarkColorScheme() {
        val scheme = AppTheme.colorScheme(isDark = true)
        assertNotNull(scheme.primary)
        assertNotNull(scheme.secondary)
    }

    @Test
    fun testTypography() {
        val typography = AppTheme.typography()
        assertNotNull(typography)
    }
}
