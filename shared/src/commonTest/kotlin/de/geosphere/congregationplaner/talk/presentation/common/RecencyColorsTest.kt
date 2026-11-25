package de.geosphere.congregationplaner.talk.presentation.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import de.geosphere.congregationplaner.talk.domain.model.RecencyLevel
import de.geosphere.congregationplaner.theming.recencyDarkPalette
import de.geosphere.congregationplaner.theming.recencyLightPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecencyColorsTest {
    @Test
    fun `each level maps to its own color in both palettes`() {
        for (palette in listOf(recencyDarkPalette, recencyLightPalette)) {
            assertEquals(RecencyLevel.entries.size, RecencyLevel.entries.map { palette.colorOf(it) }.toSet().size)
        }
    }

    @Test
    fun `tile text has enough contrast on every colored level`() {
        for (palette in listOf(recencyDarkPalette, recencyLightPalette)) {
            RecencyLevel.entries.filter { it != RecencyLevel.NEVER }.forEach { level ->
                val ratio = contrast(palette.onTile, palette.colorOf(level))
                assertTrue(ratio >= MIN_CONTRAST, "$level: $ratio")
            }
        }
    }

    @Test
    fun `colored text has enough contrast on the light background`() {
        RecencyLevel.entries.forEach { level ->
            val ratio = contrast(recencyLightPalette.colorOf(level), Color.White)
            assertTrue(ratio >= MIN_CONTRAST, "$level: $ratio")
        }
    }

    private fun contrast(a: Color, b: Color): Float {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05f) / (dark + 0.05f)
    }

    private companion object {
        const val MIN_CONTRAST = 4.5f
    }
}
