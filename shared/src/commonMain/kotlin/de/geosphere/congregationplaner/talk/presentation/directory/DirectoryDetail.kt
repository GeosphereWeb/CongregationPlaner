package de.geosphere.congregationplaner.talk.presentation.directory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightCardColors
import de.geosphere.congregationplaner.ui.field.FormTextField
import de.geosphere.congregationplaner.ui.form.EditorActions
import de.geosphere.congregationplaner.ui.pane.DetailPlaceholder

/** Was nach der Rückfrage gelöscht wird. */
internal class DeleteRequest(val text: String, val confirm: () -> Unit)

@Composable
internal fun ExternalDirectoryDetail(
    state: ExternalDirectoryUiState,
    actions: ExternalDirectoryActions,
    isBusy: Boolean,
    onRequestDelete: (DeleteRequest) -> Unit,
) {
    val directory = state.directory
    val editor = state.editor
    if (editor != null && !state.canPlan) {
        ReadOnlyCard(readOnlyView(editor, directory))
        return
    }
    when (editor) {
        null -> DetailPlaceholder("Wähle einen Kreis, eine Versammlung oder einen Redner aus.")

        is ExternalDirectoryEditor.Circuit -> EditorCard(
            title = if (editor.original == null) "Neuer Kreis" else "Kreis bearbeiten",
            editor = editor,
            state = state,
            actions = actions,
            isBusy = isBusy,
            onDelete = editor.original?.let { circuit ->
                {
                    onRequestDelete(
                        DeleteRequest("Kreis „${circuit.name}“ wird gelöscht.") { actions.onDeleteCircuit(circuit) },
                    )
                }
            },
        ) {
            FormTextField("Name des Kreises", editor.circuit.name) {
                actions.onEditorChange(editor.copy(circuit = editor.circuit.copy(name = it)))
            }
        }

        is ExternalDirectoryEditor.Congregation -> EditorCard(
            title = if (editor.original == null) "Neue Versammlung" else "Versammlung bearbeiten",
            editor = editor,
            state = state,
            actions = actions,
            isBusy = isBusy,
            onDelete = editor.original?.let { congregation ->
                {
                    onRequestDelete(
                        DeleteRequest("Versammlung „${congregation.name}“ wird gelöscht.") {
                            actions.onDeleteCongregation(congregation)
                        },
                    )
                }
            },
        ) {
            Text("Kreis: ${directory.circuitName(editor.congregation.circuitId)}")
            FormTextField("Name der Versammlung", editor.congregation.name) {
                actions.onEditorChange(editor.copy(congregation = editor.congregation.copy(name = it)))
            }
        }

        is ExternalDirectoryEditor.Speaker -> EditorCard(
            title = if (editor.original == null) "Neuer Redner" else "Redner bearbeiten",
            editor = editor,
            state = state,
            actions = actions,
            isBusy = isBusy,
            onDelete = editor.original?.let { speaker ->
                {
                    onRequestDelete(
                        DeleteRequest("Redner „${speaker.displayName()}“ wird gelöscht.") {
                            actions.onDeleteSpeaker(speaker)
                        },
                    )
                }
            },
        ) {
            SpeakerFields(editor, directory) { actions.onEditorChange(it) }
        }
    }
}

/** Nur-Lesen-Ansicht für den Helfer: dieselben Angaben wie im Editor, aber ohne Felder und Aktionen. */
@Composable
internal fun ReadOnlyCard(view: ReadOnlyView) {
    Card(modifier = Modifier.fillMaxWidth(), colors = nightCardColors()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(view.title, style = MaterialTheme.typography.titleMedium)
            view.lines.forEach { Text(it) }
        }
    }
}

@Composable
@Suppress("LongParameterList")
internal fun EditorCard(
    title: String,
    editor: ExternalDirectoryEditor,
    state: ExternalDirectoryUiState,
    actions: ExternalDirectoryActions,
    isBusy: Boolean,
    onDelete: (() -> Unit)?,
    fields: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = nightCardColors()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            fields()
            EditorActions(
                saveText = if (state.isSaving) "Speichert …" else "Speichern",
                saveEnabled = !isBusy && editor.isDirty,
                discardEnabled = !isBusy,
                onSave = actions.onSave,
                onDiscard = actions.onDiscard,
                // Nur ein gespeicherter Eintrag lässt sich löschen; ein neuer ist noch nicht angelegt.
                onDelete = onDelete,
                deleteEnabled = !isBusy,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun ExternalDirectoryDetailPreview() = PreviewThemeWrapper {
    Column {
        ExternalDirectoryDetail(previewDirectoryState, ExternalDirectoryActions(), isBusy = false, onRequestDelete = {})
        // Nur sehen: der Eintrag erscheint ohne Felder.
        ExternalDirectoryDetail(
            previewDirectoryState.copy(canPlan = false),
            ExternalDirectoryActions(),
            isBusy = false,
            onRequestDelete = {},
        )
    }
}
