package de.geosphere.congregationplaner.notice.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.notice.domain.model.NoticeImage
import de.geosphere.congregationplaner.notice.domain.model.TalkNoticeSettings
import de.geosphere.congregationplaner.talk.presentation.schedule.TalkDateInput
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.button.PrimaryButton
import de.geosphere.congregationplaner.ui.button.SecondaryButton
import kotlinx.datetime.LocalDate

/** Section "Aushang (PDF)": creating the notice and the congregation settings; planners on Android/desktop only. */
@Composable
internal fun TalkNoticeContent(state: TalkNoticeUiState, actions: TalkNoticeActions, modifier: Modifier = Modifier) {
    if (!state.canManage || !state.isAvailable) return
    Column(modifier.widthIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("Aushang (PDF)", style = MaterialTheme.typography.titleMedium)
        CreateSection(state, actions)
        TalkNoticeSettingsContent(state, actions)
    }
    state.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = actions.onDismissError,
            title = { Text("Fehler") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = actions.onDismissError) { Text("OK") } },
        )
    }
}

@Composable
private fun CreateSection(state: TalkNoticeUiState, actions: TalkNoticeActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Erstellen", style = MaterialTheme.typography.titleSmall)
        state.start?.let { TalkDateInput(it, "Startdatum", actions.onChangeStart) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Anzahl", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = actions.onDecreaseCount, enabled = state.canDecreaseCount) { Text("−") }
            Text(state.count.toString(), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = actions.onIncreaseCount, enabled = state.canIncreaseCount) { Text("+") }
        }
        Text("Grafik", style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.selectedImageId == null,
                onClick = { actions.onSelectImage(null) },
                label = { Text("Keine Grafik") },
            )
            state.images.forEach { image ->
                FilterChip(
                    selected = state.selectedImageId == image.id,
                    onClick = { actions.onSelectImage(image.id) },
                    label = { Text(image.name) },
                )
            }
        }
        PrimaryButton(
            "PDF erstellen",
            actions.onCreatePdf,
            Modifier.fillMaxWidth(),
            enabled = !state.isBusy && state.start != null,
        )
        state.savedFile?.let { file ->
            Text("Gespeichert: $file", style = MaterialTheme.typography.bodyMedium)
            SecondaryButton("Schließen", actions.onDismissSaved, outlined = false)
        }
    }
}

private val previewImages = listOf(
    NoticeImage("a", "Saal", byteArrayOf(1)),
    NoticeImage("b", "Logo", byteArrayOf(2)),
)

private val previewPlanner = TalkNoticeUiState(
    canManage = true,
    isAvailable = true,
    settings = TalkNoticeSettings(title = "Zusammenkünfte für die Öffentlichkeit", defaultImageId = "a"),
    titleDraft = "Zusammenkünfte für die Öffentlichkeit",
    images = previewImages,
    start = LocalDate(2026, 12, 6),
    selectedImageId = "a",
)

@ThemePreviews
@Composable
private fun TalkNoticeContentPreview() = PreviewThemeWrapper {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        TalkNoticeContent(previewPlanner.copy(savedFile = "Aushang_2026-12-06.pdf"), TalkNoticeActions())
        TalkNoticeContent(
            previewPlanner.copy(images = emptyList(), selectedImageId = null, settings = TalkNoticeSettings()),
            TalkNoticeActions(),
        )
        TalkNoticeContent(previewPlanner.copy(isBusy = true), TalkNoticeActions())
    }
}

@ThemePreviews
@Composable
private fun TalkNoticeErrorPreview() = PreviewThemeWrapper {
    TalkNoticeContent(
        previewPlanner.copy(errorMessage = "Es sind höchstens 10 Grafiken möglich."),
        TalkNoticeActions(),
    )
}

@ThemePreviews
@Composable
private fun TalkNoticeDeletePreview() = PreviewThemeWrapper {
    TalkNoticeContent(previewPlanner.copy(pendingDeleteImage = previewImages.first()), TalkNoticeActions())
}
