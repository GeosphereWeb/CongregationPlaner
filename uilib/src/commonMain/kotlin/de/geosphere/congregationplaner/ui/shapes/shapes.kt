@file:Suppress("ktlint:standard:filename")

package de.geosphere.congregationplaner.ui.shapes

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import de.geosphere.congregationplaner.theming.AppDimensions
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

@Suppress("ComposableNaming")
@Composable
fun ButtonShape(): Shape {
    val btnShape = RoundedCornerShape(AppDimensions.CornerRadiusLarge)
    return btnShape
}

@ThemePreviews
@Composable
private fun ButtonShapePreview() = PreviewThemeWrapper {
    Button(onClick = {}, shape = ButtonShape()) {
        Text("Schaltfläche mit ButtonShape")
    }
}
