package de.geosphere.congregationplaner.ui.list

import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.nightPalette
import de.geosphere.congregationplaner.theming.paletteOf
import de.geosphere.congregationplaner.theming.planerDarkColors
import de.geosphere.congregationplaner.ui.chip.StatusTone
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Highlight and chip colors must follow a theme switch instead of keeping the palette active at first use. */
class ThemeTokenTest {
    @AfterTest
    fun tearDown() {
        NightColors.palette = nightPalette
    }

    @Test
    fun `card highlight and status tone follow the active palette`() {
        NightColors.palette = nightPalette
        assertEquals(nightPalette.skyContainer, CardHighlight.Info.container)
        assertEquals(nightPalette.goldContainer, StatusTone.Gold.container)

        val blueDark = paletteOf(planerDarkColors, dark = true)
        NightColors.palette = blueDark

        assertEquals(blueDark.skyContainer, CardHighlight.Info.container)
        assertEquals(blueDark.coral, CardHighlight.Warning.border)
        assertEquals(blueDark.goldContainer, StatusTone.Gold.container)
        assertEquals(blueDark.mint, StatusTone.Mint.content)
    }
}
