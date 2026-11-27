package de.geosphere.congregationplaner.notice.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.notice.domain.model.NoticeColorSet
import de.geosphere.congregationplaner.notice.domain.model.NoticeImage
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.ui.button.PrimaryButton
import de.geosphere.congregationplaner.ui.button.SecondaryButton
import de.geosphere.congregationplaner.ui.dialog.ConfirmDialog
import de.geosphere.congregationplaner.ui.field.FormTextField
import de.geosphere.congregationplaner.ui.icon.deleteIcon
import de.geosphere.congregationplaner.ui.icon.editIcon
import org.jetbrains.compose.resources.decodeToImageBitmap

/** "Einstellungen der Versammlung": title, color set and graphics of the notice. */
@Composable
internal fun TalkNoticeSettingsContent(state: TalkNoticeUiState, actions: TalkNoticeActions) {
    var renaming by remember { mutableStateOf<NoticeImage?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Einstellungen der Versammlung", style = MaterialTheme.typography.titleSmall)
        FormTextField("Titel", state.titleDraft, !state.isBusy, actions.onEditTitle)
        SecondaryButton("Speichern", actions.onSaveTitle, enabled = !state.isBusy, outlined = false)
        Text("Farbset", style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NoticeColorSet.entries.forEach { set ->
                FilterChip(
                    selected = state.settings.colorSet == set,
                    onClick = { actions.onChooseColorSet(set) },
                    enabled = !state.isBusy,
                    label = { Text(set.label) },
                    leadingIcon = { Swatch(set) },
                )
            }
        }
        Text("Grafiken", style = MaterialTheme.typography.bodyMedium)
        state.images.forEach { image ->
            ImageRow(image, image.id == state.settings.defaultImageId, !state.isBusy, actions) { renaming = image }
        }
        PrimaryButton(
            "Grafik hinzufügen",
            actions.onAddImage,
            Modifier.fillMaxWidth(),
            enabled = state.canAddImage,
        )
    }
    renaming?.let { image ->
        RenameDialog(
            image,
            onConfirm = { name ->
                actions.onRenameImage(image.id, name)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
    state.pendingDeleteImage?.let { image ->
        ConfirmDialog(
            title = "Grafik löschen?",
            message = "„${image.name}“ wird dauerhaft gelöscht.",
            confirmText = "Löschen",
            onConfirm = actions.onConfirmDeleteImage,
            onDismiss = { actions.onRequestDeleteImage(null) },
            isDangerous = true,
        )
    }
}

@Composable
private fun Swatch(set: NoticeColorSet) {
    Row {
        Box(Modifier.size(14.dp).clip(CircleShape).background(Color(set.primary)))
        Box(Modifier.size(14.dp).clip(CircleShape).background(Color(set.stripe)))
    }
}

@Composable
private fun ImageRow(
    image: NoticeImage,
    isDefault: Boolean,
    enabled: Boolean,
    actions: TalkNoticeActions,
    onRename: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Thumbnail(image)
        Column(Modifier.weight(1f)) {
            Text(image.name, style = MaterialTheme.typography.bodyMedium)
            if (isDefault) {
                Text(
                    "Standard",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                TextButton(onClick = { actions.onMakeDefault(image.id) }, enabled = enabled) { Text("Als Standard") }
            }
        }
        IconButton(onClick = onRename, enabled = enabled, modifier = Modifier.size(40.dp)) {
            Icon(editIcon, contentDescription = "Umbenennen", tint = NightColors.Lavender)
        }
        IconButton(
            onClick = { actions.onRequestDeleteImage(image) },
            enabled = enabled,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(deleteIcon, contentDescription = "Löschen", tint = NightColors.Coral)
        }
    }
}

/** A 40 dp preview of the PNG; an undecodable file leaves the box empty (name only). */
@Composable
private fun Thumbnail(image: NoticeImage) {
    val bitmap = remember(image.id) { runCatching { image.png.decodeToImageBitmap() }.getOrNull() }
    Box(Modifier.size(40.dp)) {
        bitmap?.let {
            Image(it, contentDescription = image.name, modifier = Modifier.size(40.dp), contentScale = ContentScale.Fit)
        }
    }
}

@Composable
private fun RenameDialog(image: NoticeImage, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember(image.id) { mutableStateOf(image.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Grafik umbenennen") },
        text = { FormTextField("Name", name) { name = it } },
        confirmButton = { TextButton(onClick = { onConfirm(name) }) { Text("Speichern") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
