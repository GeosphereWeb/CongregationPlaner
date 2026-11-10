package de.geosphere.congregationplaner.talk.presentation.directory

import de.geosphere.congregationplaner.presentation.state.UiStateStore
import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker

/** Öffnet, ändert und schließt den Eintrag im Detail. */
class DirectoryEditorHolder(private val store: UiStateStore<ExternalDirectoryUiState>) {
    fun newCircuit() = open(ExternalDirectoryEditor.Circuit(ExternalCircuit(name = "")))

    fun newCongregation(circuitId: String) =
        open(ExternalDirectoryEditor.Congregation(ExternalCongregation(circuitId = circuitId, name = "")))

    fun newSpeaker(congregationId: String) =
        open(ExternalDirectoryEditor.Speaker(VisitingSpeaker(congregationId = congregationId, lastName = ""), ""))

    fun select(circuit: ExternalCircuit) = open(ExternalDirectoryEditor.Circuit(circuit, circuit))

    fun select(congregation: ExternalCongregation) =
        open(ExternalDirectoryEditor.Congregation(congregation, congregation))

    fun select(speaker: VisitingSpeaker) =
        open(ExternalDirectoryEditor.Speaker(speaker, speaker.talkNumbers.toText(), speaker))

    /** Während des Speicherns bleibt der Editor unverändert, sonst würde das Ergebnis die Eingaben überschreiben. */
    fun update(editor: ExternalDirectoryEditor) = store.update {
        if (it.isSaving || it.editor == null || !it.canPlan) it else it.copy(editor = editor)
    }

    fun close() = store.update { it.copy(editor = null, errorMessage = null) }

    /** Verwirft Änderungen, lässt aber den gespeicherten Eintrag geöffnet; ein neuer Eintrag wird geschlossen. */
    fun discard() = store.update { state -> state.copy(editor = state.editor?.reverted(), errorMessage = null) }

    private fun open(editor: ExternalDirectoryEditor) {
        val state = store.value
        // Wer nur sehen darf, öffnet vorhandene Einträge; neue (ohne ID) legt nur der Planer an.
        val allowed = if (editor.id == null) state.canPlan else state.canView
        if (state.isSaving || state.isDeleting || !allowed) return
        store.update { it.copy(editor = editor, errorMessage = null) }
    }
}

/** Der gespeicherte Stand des Eintrags; `null` für einen neuen Eintrag. */
internal fun ExternalDirectoryEditor.reverted(): ExternalDirectoryEditor? = when (this) {
    is ExternalDirectoryEditor.Circuit -> original?.let { copy(circuit = it) }
    is ExternalDirectoryEditor.Congregation -> original?.let { copy(congregation = it) }
    is ExternalDirectoryEditor.Speaker -> original?.let { copy(speaker = it, talksText = it.talkNumbers.toText()) }
}
