package de.geosphere.congregationplaner.user.presentation.management

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfile

/** Mehrfachauswahl der Redner aus dem Verzeichnis, die Mitglieder der eigenen Versammlung werden. */
@Composable
internal fun AdoptSpeakersDialog(
    dialog: AdoptSpeakersDialogState,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Redner als Mitglieder übernehmen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Die gewählten Redner werden Personen ohne Konto in deiner Versammlung; ihre geplanten Termine " +
                        "zeigen danach auf sie, und sie verschwinden aus dem Rednerverzeichnis.",
                    style = MaterialTheme.typography.bodySmall,
                )
                when {
                    dialog.isLoading -> CircularProgressIndicator()
                    dialog.speakers.isEmpty() -> Text("Das Rednerverzeichnis ist leer.")
                    else -> LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(dialog.speakers, key = { it.id }) { speaker ->
                            Row(
                                Modifier.fillMaxWidth().clickable(enabled = !dialog.isWorking) { onToggle(speaker.id) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = speaker.id in dialog.selected,
                                    onCheckedChange = { onToggle(speaker.id) },
                                    enabled = !dialog.isWorking,
                                )
                                Column {
                                    Text(speaker.name)
                                    Text(speaker.congregation, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = dialog.canConfirm) {
                Text(if (dialog.isWorking) "Übernimmt …" else "Übernehmen (${dialog.selected.size})")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !dialog.isWorking) { Text("Abbrechen") } },
    )
}

/** Auswahl der Person ohne Konto, deren Angaben und Termine auf das Konto übergehen. */
@Composable
internal fun LinkAccountDialog(
    dialog: LinkAccountDialogState,
    candidates: List<AppUser>,
    onSelect: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mit Person ohne Konto verknüpfen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Die Angaben der gewählten Person gehen auf das Konto ${dialog.account.label} über, ihre Termine " +
                        "zeigen danach auf das Konto, und die Person ohne Konto wird entfernt.",
                    style = MaterialTheme.typography.bodySmall,
                )
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(candidates, key = { it.uid }) { person ->
                        Row(
                            Modifier.fillMaxWidth().clickable(enabled = !dialog.isWorking) { onSelect(person.uid) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = person.uid == dialog.selectedPersonId,
                                onClick = { onSelect(person.uid) },
                                enabled = !dialog.isWorking,
                            )
                            Text(person.label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = dialog.canConfirm) {
                Text(if (dialog.isWorking) "Verknüpft …" else "Verknüpfen")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !dialog.isWorking) { Text("Abbrechen") } },
    )
}

@ThemePreviews
@Composable
private fun AdoptSpeakersDialogPreview() = PreviewThemeWrapper {
    AdoptSpeakersDialog(
        dialog = AdoptSpeakersDialogState(
            speakers = listOf(SpeakerChoice("1", "Berg, Lea", "Musterstadt"), SpeakerChoice("2", "Kern, Tom", "Nord")),
            selected = setOf("1"),
            isLoading = false,
        ),
        onToggle = {},
        onConfirm = {},
        onDismiss = {},
    )
}

@ThemePreviews
@Composable
private fun AdoptSpeakersDialogLoadingPreview() = PreviewThemeWrapper {
    AdoptSpeakersDialog(dialog = AdoptSpeakersDialogState(), onToggle = {}, onConfirm = {}, onDismiss = {})
}

@ThemePreviews
@Composable
private fun LinkAccountDialogPreview() = PreviewThemeWrapper {
    LinkAccountDialog(
        dialog = LinkAccountDialogState(AppUser(uid = "a", email = "max@example.com"), selectedPersonId = "p1"),
        candidates = listOf(
            AppUser(uid = "p1", hasAccount = false, profile = MemberProfile(lastName = "Muster", firstName = "Max")),
            AppUser(uid = "p2", hasAccount = false, profile = MemberProfile(lastName = "Kind", firstName = "Lena")),
        ),
        onSelect = {},
        onConfirm = {},
        onDismiss = {},
    )
}
