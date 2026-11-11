package de.geosphere.congregationplaner.talk.presentation.directory

import de.geosphere.congregationplaner.presentation.state.UiStateStore
import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteExternalCircuitUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteExternalCongregationUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteVisitingSpeakerUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveExternalCircuitUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveExternalCongregationUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveVisitingSpeakerUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Die UseCases, die das Verzeichnis schreiben. */
@Inject
class DirectoryChangeUseCases(
    val saveCircuit: SaveExternalCircuitUseCase,
    val saveCongregation: SaveExternalCongregationUseCase,
    val saveSpeaker: SaveVisitingSpeakerUseCase,
    val deleteCircuit: DeleteExternalCircuitUseCase,
    val deleteCongregation: DeleteExternalCongregationUseCase,
    val deleteSpeaker: DeleteVisitingSpeakerUseCase,
)

/** Speichert und löscht Kreise, Versammlungen und Redner; die Liste übernimmt das Ergebnis sofort (ADR-0006). */
class DirectoryChangesHolder(
    private val store: UiStateStore<ExternalDirectoryUiState>,
    private val scope: CoroutineScope,
    private val useCases: DirectoryChangeUseCases,
    private val currentUser: () -> AppUser?,
) {
    fun save() {
        val state = store.value
        if (state.isSaving || state.isDeleting) return
        when (val editor = state.editor) {
            is ExternalDirectoryEditor.Circuit ->
                run(SAVE, "Kreis konnte nicht gespeichert werden.", { useCases.saveCircuit(it, editor.circuit) }) {
                        current,
                        saved,
                    ->
                    current.withDirectory { it.copy(circuits = it.circuits.upsert(saved) { c -> c.id }) }
                        .copy(editor = ExternalDirectoryEditor.Circuit(saved, saved))
                }

            is ExternalDirectoryEditor.Congregation -> run(
                SAVE,
                "Versammlung konnte nicht gespeichert werden.",
                { useCases.saveCongregation(it, editor.congregation) },
            ) { current, saved ->
                current.withDirectory { it.copy(congregations = it.congregations.upsert(saved) { c -> c.id }) }
                    .copy(editor = ExternalDirectoryEditor.Congregation(saved, saved))
            }

            is ExternalDirectoryEditor.Speaker -> saveSpeaker(editor)
            null -> Unit
        }
    }

    fun delete(circuit: ExternalCircuit) =
        run(DELETE, "Kreis konnte nicht gelöscht werden.", { useCases.deleteCircuit(it, circuit.id) }) { current, _ ->
            current.withDirectory { it.copy(circuits = it.circuits.filterNot { c -> c.id == circuit.id }) }
                .closing { it is ExternalDirectoryEditor.Circuit && it.id == circuit.id }
        }

    fun delete(congregation: ExternalCongregation) = run(
        DELETE,
        "Versammlung konnte nicht gelöscht werden.",
        { useCases.deleteCongregation(it, congregation.id) },
    ) { current, _ ->
        current.withDirectory { it.copy(congregations = it.congregations.filterNot { c -> c.id == congregation.id }) }
            .closing { it is ExternalDirectoryEditor.Congregation && it.id == congregation.id }
    }

    fun delete(speaker: VisitingSpeaker) =
        run(DELETE, "Redner konnte nicht gelöscht werden.", { useCases.deleteSpeaker(it, speaker.id) }) { current, _ ->
            current.withDirectory { it.copy(speakers = it.speakers.filterNot { s -> s.id == speaker.id }) }
                .closing { it is ExternalDirectoryEditor.Speaker && it.id == speaker.id }
        }

    private fun saveSpeaker(editor: ExternalDirectoryEditor.Speaker) {
        val numbers = parseTalkNumbers(editor.talksText)
        if (numbers == null) {
            store.update { it.copy(errorMessage = "Vortragsnummern ungültig: nur Zahlen, durch Komma getrennt.") }
            return
        }
        run(
            SAVE,
            "Redner konnte nicht gespeichert werden.",
            { useCases.saveSpeaker(it, editor.speaker.copy(talkNumbers = numbers)) },
        ) { current, saved ->
            current.withDirectory { it.copy(speakers = it.speakers.upsert(saved) { s -> s.id }) }
                .copy(editor = ExternalDirectoryEditor.Speaker(saved, saved.talkNumbers.toText(), saved))
        }
    }

    /** Führt [call] aus; [deleting] unterscheidet, welche Markierung läuft. Danach übernimmt [onDone] das Ergebnis. */
    private fun <T> run(
        deleting: Boolean,
        failureMessage: String,
        call: suspend (AppUser) -> Result<T>,
        onDone: (ExternalDirectoryUiState, T) -> ExternalDirectoryUiState,
    ) {
        val user = currentUser() ?: return
        if (deleting && (store.value.isSaving || store.value.isDeleting)) return
        store.update { it.busy(deleting, true).copy(errorMessage = null) }
        scope.launch {
            val result = call(user)
            store.update { state ->
                result.fold(
                    onSuccess = { onDone(state, it).busy(deleting, false) },
                    onFailure = { state.busy(deleting, false).copy(errorMessage = it.message ?: failureMessage) },
                )
            }
        }
    }

    private companion object {
        const val SAVE = false
        const val DELETE = true
    }
}

private fun ExternalDirectoryUiState.busy(deleting: Boolean, busy: Boolean) =
    if (deleting) copy(isDeleting = busy) else copy(isSaving = busy)

private fun ExternalDirectoryUiState.withDirectory(change: (ExternalDirectory) -> ExternalDirectory) =
    copy(directory = change(directory))

private fun ExternalDirectoryUiState.closing(matches: (ExternalDirectoryEditor) -> Boolean) =
    copy(editor = editor?.takeUnless(matches))
