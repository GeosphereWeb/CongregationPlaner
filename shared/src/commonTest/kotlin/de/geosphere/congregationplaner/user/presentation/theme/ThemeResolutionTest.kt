package de.geosphere.congregationplaner.user.presentation.theme

import de.geosphere.congregationplaner.theming.ThemeMode
import de.geosphere.congregationplaner.theming.ThemeStyle
import de.geosphere.congregationplaner.theming.isDark
import de.geosphere.congregationplaner.theming.paletteOf
import de.geosphere.congregationplaner.theming.planerDarkColors
import de.geosphere.congregationplaner.theming.planerLightColors
import de.geosphere.congregationplaner.theming.planerPurpleDarkColors
import de.geosphere.congregationplaner.theming.soberColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Theme resolution lives in `theming`; tested here because the pipeline only runs the `shared` tests. */
class ThemeResolutionTest {
    @Test
    fun `system mode follows the device and fixed modes ignore it`() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemDark = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemDark = false))
        assertFalse(ThemeMode.LIGHT.isDark(systemDark = true))
        assertTrue(ThemeMode.DARK.isDark(systemDark = false))
    }

    @Test
    fun `night style has no sober scheme and the others pick light or dark`() {
        assertNull(soberColorScheme(ThemeStyle.NIGHT, dark = false))
        assertEquals(planerLightColors, soberColorScheme(ThemeStyle.BLUE, dark = false))
        assertEquals(planerDarkColors, soberColorScheme(ThemeStyle.BLUE, dark = true))
        assertEquals(planerPurpleDarkColors, soberColorScheme(ThemeStyle.PURPLE, dark = true))
    }

    @Test
    fun `sober palette uses the scheme colors without header gradient or brand fonts`() {
        val palette = paletteOf(planerLightColors, dark = false)

        assertEquals(planerLightColors.background, palette.background)
        assertEquals(planerLightColors.onSurface, palette.onBackground)
        assertEquals(palette.headerTop, palette.headerBottom)
        assertFalse(palette.brandFonts)
        assertFalse(palette.headerGlow)
    }
}
