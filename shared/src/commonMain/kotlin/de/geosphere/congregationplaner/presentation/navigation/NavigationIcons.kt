package de.geosphere.congregationplaner.presentation.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// Die Pfade stammen aus den Material Symbols (Apache 2.0): home, group, public, tune.
private fun navigationIcon(name: String, pathData: String): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = addPathNodes(pathData),
    fill = SolidColor(Color.Black),
).build()

private val homeIcon = navigationIcon("home", "M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z")

private val groupIcon = navigationIcon(
    "group",
    "M16,11c1.66,0 2.99,-1.34 2.99,-3S17.66,5 16,5c-1.66,0 -3,1.34 -3,3s1.34,3 3,3zM8,11c1.66,0 2.99,-1.34 " +
        "2.99,-3S9.66,5 8,5C6.34,5 5,6.34 5,8s1.34,3 3,3zM8,13c-2.33,0 -7,1.17 -7,3.5L1,19h14v-2.5c0,-2.33 " +
        "-4.67,-3.5 -7,-3.5zM16,13c-0.29,0 -0.62,0.02 -0.97,0.05 1.16,0.84 1.97,1.97 1.97,3.45L17,19h6v-2.5c0," +
        "-2.33 -4.67,-3.5 -7,-3.5z",
)

private val globeIcon = navigationIcon(
    "public",
    "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM4,12c0,-0.61 0.08,-1.21 0.21,-1.78L8.99," +
        "15v1c0,1.1 0.9,2 2,2v1.93C7.06,19.43 4,16.07 4,12zM17.89,17.4c-0.26,-0.81 -1,-1.4 -1.9,-1.4h-1v-3c0," +
        "-0.55 -0.45,-1 -1,-1h-6v-2h2c0.55,0 1,-0.45 1,-1V7h2c1.1,0 2,-0.9 2,-2v-0.41C17.92,5.77 20,8.65 20,12c0," +
        "2.08 -0.8,3.97 -2.11,5.4z",
)

private val tuneIcon = navigationIcon(
    "tune",
    "M3,17v2h6v-2L3,17zM3,5v2h10L13,5L3,5zM13,21v-2h8v-2h-8v-2h-2v6h2zM7,9v2L3,11v2h4v2h2L9,9L7,9zM21,13v-2L11," +
        "11v2h10zM15,9h2L17,7h4L21,5h-4L17,3h-2v6z",
)

val TopLevelDestination.icon: ImageVector
    get() = when (this) {
        TopLevelDestination.HOME -> homeIcon
        TopLevelDestination.CONGREGATION -> groupIcon
        TopLevelDestination.GLOBAL_ADMIN -> globeIcon
        TopLevelDestination.SETTINGS -> tuneIcon
    }

internal val arrowBackIcon =
    navigationIcon("arrow_back", "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z")
