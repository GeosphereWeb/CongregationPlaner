package de.geosphere.congregationplaner.talk.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.presentation.components.ListDetailState
import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteExternalCircuitUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteExternalCongregationUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteVisitingSpeakerUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveExternalDirectoryUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveExternalCircuitUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveExternalCongregationUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveVisitingSpeakerUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Der Eintrag im Detail; `original` ist `null`, solange der Eintrag neu und noch nicht gespeichert ist. */
sealed interface ExternalDirectoryEditor {
    val id: String?
    val isDirty: Boolean

    data class Circuit(val circuit: ExternalCircuit, val original: ExternalCircuit? = null) :
        ExternalDirectoryEditor {
        override val id: String? get() = original?.id
        override val isDirty: Boolean get() = circuit != original
    }

    data class Congregation(val congregation: ExternalCongregation, val original: ExternalCongregation? = null) :
        ExternalDirectoryEditor {
        override val id: String? get() = original?.id
        override val isDirty: Boolean get() = congregation != original
    }

    /** [talksText] ist die Eingabe der Vortragsnummern; sie wird erst beim Speichern in Zahlen umgewandelt. */
    data class Speaker(val speaker: VisitingSpeaker, val talksText: String, val original: VisitingSpeaker? = null) :
        ExternalDirectoryEditor {
        override val id: String? get() = original?.id
        override val isDirty: Boolean
            get() = speaker != original || talksText != speaker.talkNumbers.toText()
    }
}

internal fun List<Int>.toText(): String = joinToString(", ")

/** Zahlen getrennt durch Komma, Semikolon oder Leerzeichen; `null`, wenn ein Eintrag keine Zahl ist. */
internal fun parseTalkNumbers(text: String): List<Int>? {
    val numbers = text.split(',', ';', ' ').filter(String::isNotBlank).map { it.toIntOrNull() ?: return null }
    return numbers.distinct().sorted()
}

data class ExternalDirectoryUiState(
    val directory: ExternalDirectory = ExternalDirectory(),
    val editor: ExternalDirectoryEditor? = null,
    val isLoading: Boolean = true,
    val canPlan: Boolean = false,
    override val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    override val errorMessage: String? = null,
) : ListDetailState {
    override val hasDetail: Boolean get() = editor != null
}

/** Pflege des Verzeichnisses externer Redner (Kreis → Versammlung → Redner) durch die Planer der Versammlung. */
@Inject
class ExternalDirectoryViewModel(
    private val observeDirectory: ObserveExternalDirectoryUseCase,
    private val saveCircuit: SaveExternalCircuitUseCase,
    private val saveCongregation: SaveExternalCongregationUseCase,
    private val saveSpeaker: SaveVisitingSpeakerUseCase,
    private val deleteCircuit: DeleteExternalCircuitUseCase,
    private val deleteCongregation: DeleteExternalCongregationUseCase,
    private val deleteSpeaker: DeleteVisitingSpeakerUseCase,
    private val policy: TalkSchedulePermissionPolicy,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ExternalDirectoryUiState())
    val uiState = mutableUiState.asStateFlow()

    private var currentUser: AppUser? = null
    private var observeJob: Job? = null

    fun onCurrentUserChanged(user: AppUser) {
        currentUser = user
        val canPlan = policy.canPlanTalks(user)
        mutableUiState.update { it.copy(canPlan = canPlan, editor = it.editor.takeIf { canPlan }) }
        if (canPlan) {
            observe(user)
        } else {
            observeJob?.cancel()
            observeJob = null
            mutableUiState.update { it.copy(directory = ExternalDirectory(), isLoading = false) }
        }
    }

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
    fun updateEditor(editor: ExternalDirectoryEditor) {
        mutableUiState.update { if (it.isSaving || it.editor == null) it else it.copy(editor = editor) }
    }

    fun closeDetail() {
        mutableUiState.update { it.copy(editor = null, errorMessage = null) }
    }

    /** Verwirft Änderungen, lässt aber den gespeicherten Eintrag geöffnet; ein neuer Eintrag wird geschlossen. */
    fun discardChanges() {
        mutableUiState.update { state ->
            val reset = when (val editor = state.editor) {
                is ExternalDirectoryEditor.Circuit -> editor.original?.let { editor.copy(circuit = it) }
                is ExternalDirectoryEditor.Congregation -> editor.original?.let { editor.copy(congregation = it) }
                is ExternalDirectoryEditor.Speaker ->
                    editor.original?.let { editor.copy(speaker = it, talksText = it.talkNumbers.toText()) }
                null -> null
            }
            state.copy(editor = reset, errorMessage = null)
        }
    }

    fun dismissError() {
        mutableUiState.update { it.copy(errorMessage = null) }
        // Nach einem Ladefehler erneut laden.
        currentUser?.takeIf(policy::canPlanTalks)?.let(::observe)
    }

    fun save() {
        val state = mutableUiState.value
        if (state.isSaving || state.isDeleting) return
        when (val editor = state.editor) {
            is ExternalDirectoryEditor.Circuit -> runSave(
                "Kreis konnte nicht gespeichert werden.",
                { saveCircuit(it, editor.circuit) },
            ) { current, saved ->
                current.copy(
                    directory = current.directory.copy(circuits = current.directory.circuits.upsert(saved) { it.id }),
                    editor = ExternalDirectoryEditor.Circuit(saved, saved),
                )
            }

            is ExternalDirectoryEditor.Congregation -> runSave(
                "Versammlung konnte nicht gespeichert werden.",
                { saveCongregation(it, editor.congregation) },
            ) { current, saved ->
                current.copy(
                    directory = current.directory.copy(
                        congregations = current.directory.congregations.upsert(saved) { it.id },
                    ),
                    editor = ExternalDirectoryEditor.Congregation(saved, saved),
                )
            }

            is ExternalDirectoryEditor.Speaker -> saveSpeaker(editor)
            null -> Unit
        }
    }

    fun delete(circuit: ExternalCircuit) = runDelete(
        "Kreis konnte nicht gelöscht werden.",
        { deleteCircuit(it, circuit.id) },
    ) { current ->
        current.copy(
            directory = current.directory.copy(circuits = current.directory.circuits.filterNot { it.id == circuit.id }),
            editor = current.editor?.takeUnless { it is ExternalDirectoryEditor.Circuit && it.id == circuit.id },
        )
    }

    fun delete(congregation: ExternalCongregation) = runDelete(
        "Versammlung konnte nicht gelöscht werden.",
        { deleteCongregation(it, congregation.id) },
    ) { current ->
        current.copy(
            directory = current.directory.copy(
                congregations = current.directory.congregations.filterNot { it.id == congregation.id },
            ),
            editor = current.editor?.takeUnless {
                it is ExternalDirectoryEditor.Congregation && it.id == congregation.id
            },
        )
    }

    fun delete(speaker: VisitingSpeaker) = runDelete(
        "Redner konnte nicht gelöscht werden.",
        { deleteSpeaker(it, speaker.id) },
    ) { current ->
        current.copy(
            directory = current.directory.copy(speakers = current.directory.speakers.filterNot { it.id == speaker.id }),
            editor = current.editor?.takeUnless { it is ExternalDirectoryEditor.Speaker && it.id == speaker.id },
        )
    }

    private fun saveSpeaker(editor: ExternalDirectoryEditor.Speaker) {
        val numbers = parseTalkNumbers(editor.talksText)
        if (numbers == null) {
            mutableUiState.update {
                it.copy(errorMessage = "Vortragsnummern ungültig: nur Zahlen, durch Komma getrennt.")
            }
            return
        }
        runSave(
            "Redner konnte nicht gespeichert werden.",
            { saveSpeaker(it, editor.speaker.copy(talkNumbers = numbers)) },
        ) { current, saved ->
            current.copy(
                directory = current.directory.copy(speakers = current.directory.speakers.upsert(saved) { it.id }),
                editor = ExternalDirectoryEditor.Speaker(saved, saved.talkNumbers.toText(), saved),
            )
        }
    }

    private fun open(editor: ExternalDirectoryEditor) {
        val state = mutableUiState.value
        if (state.isSaving || state.isDeleting || !state.canPlan) return
        mutableUiState.update { it.copy(editor = editor, errorMessage = null) }
    }

    private fun <T> runSave(
        failureMessage: String,
        call: suspend (AppUser) -> Result<T>,
        onSaved: (ExternalDirectoryUiState, T) -> ExternalDirectoryUiState,
    ) {
        val user = currentUser ?: return
        mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            call(user)
                .onSuccess { saved -> mutableUiState.update { onSaved(it, saved).copy(isSaving = false) } }
                .onFailure { exception ->
                    mutableUiState.update {
                        it.copy(isSaving = false, errorMessage = exception.message ?: failureMessage)
                    }
                }
        }
    }

    private fun runDelete(
        failureMessage: String,
        call: suspend (AppUser) -> Result<Unit>,
        onDeleted: (ExternalDirectoryUiState) -> ExternalDirectoryUiState,
    ) {
        val user = currentUser ?: return
        if (mutableUiState.value.isSaving || mutableUiState.value.isDeleting) return
        mutableUiState.update { it.copy(isDeleting = true, errorMessage = null) }
        viewModelScope.launch {
            call(user)
                .onSuccess { mutableUiState.update { onDeleted(it).copy(isDeleting = false) } }
                .onFailure { exception ->
                    mutableUiState.update {
                        it.copy(isDeleting = false, errorMessage = exception.message ?: failureMessage)
                    }
                }
        }
    }

    private fun observe(user: AppUser) {
        if (observeJob?.isActive == true) return
        mutableUiState.update { it.copy(isLoading = true) }
        observeJob = viewModelScope.launch {
            observeDirectory(user)
                .catch { exception ->
                    val message = exception.message ?: "Externe Redner konnten nicht geladen werden."
                    mutableUiState.update { it.copy(isLoading = false, errorMessage = message) }
                }.collect { directory ->
                    mutableUiState.update { it.copy(directory = directory, isLoading = false) }
                }
        }
    }
}

private fun <T> List<T>.upsert(item: T, id: (T) -> String): List<T> =
    if (any { id(it) == id(item) }) map { if (id(it) == id(item)) item else it } else this + item
