package de.geosphere.congregationplaner.theming

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Vorschau-Hintergrund für das Nachtblau-Design (nur dunkel). */
@Composable
fun NightPreviewWrapper(content: @Composable () -> Unit) {
    Box(modifier = Modifier.background(NightColors.Background).padding(16.dp)) {
        content()
    }
}

/** Wie [NightPreviewWrapper], aber in Handygröße für ganze Bildschirme. */
@Composable
fun NightPhonePreview(content: @Composable () -> Unit) {
    Box(modifier = Modifier.size(width = 390.dp, height = 844.dp).background(NightColors.Background)) {
        content()
    }
}
