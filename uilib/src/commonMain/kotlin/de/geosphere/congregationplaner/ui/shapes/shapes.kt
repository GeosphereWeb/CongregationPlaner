@file:Suppress("ktlint:standard:filename")

package de.geosphere.congregationplaner.ui.shapes

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import de.geosphere.congregationplaner.theming.AppDimensions

@Suppress("ComposableNaming")
@Composable
fun ButtonShape(): Shape {
    val btnShape = RoundedCornerShape(AppDimensions.CornerRadiusLarge)
    return btnShape
}
