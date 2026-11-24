package de.geosphere.congregationplaner.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Ein 24-dp-Icon aus einem SVG-Pfad; die Farbe setzt `Icon(tint = …)`. */
fun vectorIcon(name: String, pathData: String): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = addPathNodes(pathData),
    fill = SolidColor(Color.Black),
).build()

// Die Pfade stammen aus den Material Symbols (Apache 2.0): arrow_back, delete, edit, today.
val arrowBackIcon = vectorIcon("arrow_back", "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z")

val deleteIcon = vectorIcon(
    "delete",
    "M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z",
)

val editIcon = vectorIcon(
    "edit",
    "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41l-2.34,-2.34c-0.39," +
        "-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83z",
)

/** Kalender mit markiertem Tag: springt zu heute. */
val todayIcon = vectorIcon(
    "today",
    "M19,3h-1V1h-2v2H8V1H6v2H5C3.89,3 3.01,3.9 3.01,5L3,19c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2V5C21,3.9 20.1,3 19,3z" +
        "M19,19H5V8h14V19zM7,10h5v5H7V10z",
)
