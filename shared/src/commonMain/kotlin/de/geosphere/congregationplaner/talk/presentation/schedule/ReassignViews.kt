package de.geosphere.congregationplaner.talk.presentation.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** Hinweis über der Liste, solange der Zieltag fürs Umplanen gesucht wird, mit „Abbrechen“. */
@Composable
internal fun ReassignBanner(source: TalkSlot, onCancel: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            reassignBannerText(source),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onCancel) { Text("Abbrechen") }
    }
}

/** Rückfrage, ob zwei belegte Termine ihre Vorträge und Redner tauschen sollen. */
@Composable
internal fun SwapDialog(source: TalkSlot, target: TalkSlot, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Termine tauschen?") },
        text = { Text(swapQuestion(source, target)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Tauschen") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

private val previewSource = TalkSlot(
    LocalDate(2026, 11, 8),
    LocalTime(17, 30),
    talkNumber = 12,
    talkTitle = "Der Weg zum Glück",
    speakerMemberId = "1",
    speakerLastName = "Muster",
    speakerFirstName = "Max",
)

@ThemePreviews
@Composable
private fun ReassignBannerPreview() = PreviewThemeWrapper {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ReassignBanner(previewSource, onCancel = {})
    }
}

@ThemePreviews
@Composable
private fun SwapDialogPreview() = PreviewThemeWrapper {
    SwapDialog(
        source = previewSource,
        target = previewSource.copy(date = LocalDate(2026, 11, 15), talkNumber = 31, talkTitle = "Glück im Alltag"),
        onConfirm = {},
        onDismiss = {},
    )
}
