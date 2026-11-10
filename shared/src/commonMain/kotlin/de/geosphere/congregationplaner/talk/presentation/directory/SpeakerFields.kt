package de.geosphere.congregationplaner.talk.presentation.directory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.presentation.picker.label
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.field.FormTextField
import de.geosphere.congregationplaner.ui.rating.StarRating

@Composable
internal fun SpeakerFields(
    editor: ExternalDirectoryEditor.Speaker,
    directory: ExternalDirectory,
    onChange: (ExternalDirectoryEditor.Speaker) -> Unit,
) {
    val speaker = editor.speaker
    SpeakerCongregationPicker(directory, speaker.congregationId) {
        onChange(editor.copy(speaker = speaker.copy(congregationId = it)))
    }
    FormTextField("Familienname", speaker.lastName) { onChange(editor.copy(speaker = speaker.copy(lastName = it))) }
    FormTextField("Vorname", speaker.firstName) { onChange(editor.copy(speaker = speaker.copy(firstName = it))) }
    FormTextField("Handy", speaker.mobilePhone) { onChange(editor.copy(speaker = speaker.copy(mobilePhone = it))) }
    FormTextField("Festnetz", speaker.landlinePhone) {
        onChange(editor.copy(speaker = speaker.copy(landlinePhone = it)))
    }
    FormTextField("E-Mail", speaker.email) { onChange(editor.copy(speaker = speaker.copy(email = it))) }
    OutlinedTextField(
        value = editor.talksText,
        onValueChange = { onChange(editor.copy(talksText = it)) },
        label = { Text("Vorträge (Nummern)") },
        supportingText = { Text("Nummern aus der Vortragsliste, z. B. 12, 45, 101") },
        modifier = Modifier.fillMaxWidth(),
    )
    StarRating(rankingStars(speaker.ranking)) {
        onChange(editor.copy(speaker = speaker.copy(ranking = nextRanking(speaker.ranking, it))))
    }
}

/** Bewertung des Redners in Sternen; ein Tipp auf den aktuellen Stern löscht sie. */

/** Auswahl der Versammlung eines Redners; die Einträge nennen den Kreis, weil Namen doppelt vorkommen können. */
@Composable
internal fun SpeakerCongregationPicker(directory: ExternalDirectory, selectedId: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text("Versammlung", style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(directory.congregationLabel(selectedId))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            directory.congregations.forEach { congregation ->
                DropdownMenuItem(
                    text = { Text(directory.congregationLabel(congregation)) },
                    onClick = {
                        onSelect(congregation.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun SpeakerFieldsPreview() = PreviewThemeWrapper {
    Column {
        SpeakerFields(ExternalDirectoryEditor.Speaker(previewSpeaker, "12, 45", previewSpeaker), previewDirectory) {}
    }
}
