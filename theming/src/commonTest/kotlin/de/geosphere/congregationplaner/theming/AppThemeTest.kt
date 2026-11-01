package de.geosphere.congregationplaner.theming

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class AppThemeTest {

    @Test
    fun colorSchemeFollowsNightTokens() {
        val scheme = nightColorScheme()
        assertEquals(NightColors.Primary, scheme.primary)
        assertEquals(NightColors.Lavender, scheme.secondary)
        assertEquals(NightColors.Background, scheme.background)
        assertEquals(NightColors.OnBackground, scheme.onSurface)
        assertEquals(NightColors.Surface, scheme.surfaceContainer)
        assertEquals(NightColors.NavBar, scheme.surfaceContainerLow)
        assertEquals(NightColors.SurfaceRaised, scheme.surfaceContainerHigh)
        assertEquals(NightColors.Border, scheme.outlineVariant)
        assertEquals(NightColors.Coral, scheme.error)
    }

    @Test
    fun shapesFollowDesignRadii() {
        val shapes = nightShapes()
        assertEquals(RoundedCornerShape(8.dp), shapes.extraSmall)
        assertEquals(RoundedCornerShape(14.dp), shapes.medium)
        assertEquals(RoundedCornerShape(16.dp), shapes.large)
    }
}
