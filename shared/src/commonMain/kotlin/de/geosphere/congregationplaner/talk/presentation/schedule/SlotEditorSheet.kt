package de.geosphere.congregationplaner.talk.presentation.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

private const val SCRIM_ALPHA = 0.15f

/**
 * Der Termin-Editor auf schmalen Bildschirmen: ein Bottom-Sheet, das halb hoch aufgeht, damit die Planungsliste
 * darüber sichtbar bleibt; nach oben gezogen nimmt es den ganzen Bildschirm ein. Während des Speicherns lässt es sich
 * nicht schließen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SlotEditorSheet(isSaving: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val saving by rememberUpdatedState(isSaving)
    val sheetState = rememberModalBottomSheetState(confirmValueChange = { it != SheetValue.Hidden || !saving })
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = SCRIM_ALPHA),
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@ThemePreviews
@Composable
private fun SlotEditorSheetPreview() = PreviewThemeWrapper {
    SlotEditorSheet(isSaving = false, onDismiss = {}) { Text("Termin am So, 08.11.2026") }
}
