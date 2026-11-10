package de.geosphere.congregationplaner.congregation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.permission.CongregationPermissionPolicy
import de.geosphere.congregationplaner.congregation.domain.usecase.CreateCongregationUseCase
import de.geosphere.congregationplaner.congregation.domain.usecase.DeleteCongregationUseCase
import de.geosphere.congregationplaner.congregation.domain.usecase.ObserveCongregationsUseCase
import de.geosphere.congregationplaner.congregation.domain.usecase.SaveCongregationUseCase
import de.geosphere.congregationplaner.presentation.components.ListDetailState
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Aus der [CongregationPermissionPolicy] abgeleitete UI-Berechtigungen; standardmäßig ist alles verboten. */
data class CongregationPermissions(
    val canCreate: Boolean = false,
    val canManage: Boolean = false,
    val canEditSelection: Boolean = false,
    val canEditMeetingTimes: Boolean = false,
)

data class CongregationMetadataUiState(
    val congregations: List<CongregationMetadata> = emptyList(),
    val editor: CongregationMetadata? = null,
    val editorOriginal: CongregationMetadata? = null,
    val isLoading: Boolean = true,
    override val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    override val errorMessage: String? = null,
    val permissions: CongregationPermissions = CongregationPermissions(),
) : ListDetailState {
    override val hasDetail: Boolean get() = editor != null
    val isEditorDirty: Boolean get() = editor != editorOriginal
    val isBusy: Boolean get() = isSaving || isDeleting
}

@Inject
class CongregationMetadataViewModel(
    private val observeCongregations: ObserveCongregationsUseCase,
    private val saveCongregation: SaveCongregationUseCase,
    private val createCongregation: CreateCongregationUseCase,
    private val deleteCongregationUseCase: DeleteCongregationUseCase,
    private val permissionPolicy: CongregationPermissionPolicy,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(CongregationMetadataUiState())
    val uiState = mutableUiState.asStateFlow()

    private var currentUser: AppUser? = null
    private var allCongregations: List<CongregationMetadata> = emptyList()
    private var ownCongregationOnly = false
    private var circuitIds: List<String>? = null
    private var observeJob: Job? = null
    private var observing = false

    /**
     * Mit [ownOnly] zeigt die Liste nur die eigene Versammlung des Nutzers aus deren Kreis (Bereich „Versammlung“);
     * sonst alle Versammlungen des Kreises [circuitIds] (Globale Verwaltung).
     */
    fun onCurrentUserChanged(user: AppUser?, ownOnly: Boolean = false, circuitIds: List<String>? = null) {
        currentUser = user
        ownCongregationOnly = ownOnly
        val circuit = if (ownOnly) user?.congregation?.circuitIds else circuitIds
        if (!observing || circuit != this.circuitIds) observe(circuit)
        updateState {
            it.copy(congregations = visibleCongregations(allCongregations, ownCongregationOnly, currentUser))
        }
    }

    private fun observe(circuit: List<String>?) {
        observing = true
        circuitIds = circuit
        observeJob?.cancel()
        allCongregations = emptyList()
        if (circuit == null) {
            observeJob = null
            updateState { it.copy(congregations = emptyList(), isLoading = false) }
            return
        }
        updateState { it.copy(isLoading = true) }
        observeJob = viewModelScope.launch {
            observeCongregations(circuit)
                .catch { exception ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.message ?: "Versammlungen konnten nicht geladen werden.",
                        )
                    }
                }.collect { congregations ->
                    allCongregations = congregations
                    updateState {
                        it.copy(
                            congregations = visibleCongregations(allCongregations, ownCongregationOnly, currentUser),
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                }
        }
    }

    fun createCongregation() {
        if (!mutableUiState.value.permissions.canCreate) {
            updateState { it.copy(errorMessage = MISSING_PERMISSION_MESSAGE) }
            return
        }
        val empty = CongregationMetadata()
        updateState { it.copy(editor = empty, editorOriginal = empty, errorMessage = null) }
    }

    fun editCongregation(congregation: CongregationMetadata) {
        updateState { it.copy(editor = congregation, editorOriginal = congregation, errorMessage = null) }
    }

    fun updateEditor(congregation: CongregationMetadata) {
        if (!mutableUiState.value.permissions.canEditSelection) return
        updateState { it.copy(editor = congregation, errorMessage = null) }
    }

    fun cancelEditing() {
        updateState { it.copy(editor = null, editorOriginal = null, errorMessage = null) }
    }

    /**
     * Verwirft Änderungen, lässt aber die ausgewählte Versammlung geöffnet; eine neue, ungespeicherte wird
     * geschlossen.
     */
    fun discardChanges() {
        updateState { state ->
            val original = state.editorOriginal?.takeUnless { it.id.isBlank() }
            state.copy(editor = original, editorOriginal = original, errorMessage = null)
        }
    }

    fun saveCongregation() {
        if (mutableUiState.value.isSaving || mutableUiState.value.isDeleting) return
        val congregation = mutableUiState.value.editor ?: return
        val user = currentUser
        val circuit = circuitIds
        if (user == null || circuit == null) {
            updateState { it.copy(errorMessage = MISSING_PERMISSION_MESSAGE) }
            return
        }

        updateState { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val result = if (congregation.id.isBlank()) {
                createCongregation(user, circuit, congregation.name)
            } else {
                saveCongregation(user, circuit, congregation)
            }
            result
                .onSuccess { savedCongregation ->
                    updateState { state ->
                        val existingIndex = state.congregations.indexOfFirst { it.id == savedCongregation.id }
                        val congregations = if (existingIndex == -1) {
                            state.congregations + savedCongregation
                        } else {
                            state.congregations.map { current ->
                                if (current.id == savedCongregation.id) savedCongregation else current
                            }
                        }
                        state.copy(
                            congregations = congregations,
                            editor = savedCongregation,
                            editorOriginal = savedCongregation,
                            isSaving = false,
                            errorMessage = null,
                        )
                    }
                }.onFailure { exception ->
                    updateState {
                        it.copy(
                            isSaving = false,
                            errorMessage = exception.message ?: "Versammlung konnte nicht gespeichert werden.",
                        )
                    }
                }
        }
    }

    fun deleteCongregation(congregation: CongregationMetadata) {
        if (congregation.id.isBlank() || mutableUiState.value.isDeleting || mutableUiState.value.isSaving) return
        val user = currentUser
        val circuit = circuitIds
        if (user == null || circuit == null) {
            updateState { it.copy(errorMessage = MISSING_PERMISSION_MESSAGE) }
            return
        }

        updateState { it.copy(isDeleting = true, errorMessage = null) }
        viewModelScope.launch {
            deleteCongregationUseCase(user, circuit, congregation)
                .onSuccess {
                    updateState {
                        val editor = it.editor?.takeUnless { editor -> editor.id == congregation.id }
                        it.copy(
                            congregations = it.congregations.filterNot { item -> item.id == congregation.id },
                            editor = editor,
                            editorOriginal = it.editorOriginal.takeIf { editor != null },
                            isDeleting = false,
                            errorMessage = null,
                        )
                    }
                }.onFailure { exception ->
                    updateState {
                        it.copy(
                            isDeleting = false,
                            errorMessage = exception.message ?: "Versammlung konnte nicht gelöscht werden.",
                        )
                    }
                }
        }
    }

    // Berechtigungen werden bei jeder Zustandsänderung neu abgeleitet, damit sie zu Nutzer und Auswahl passen.
    private fun updateState(transform: (CongregationMetadataUiState) -> CongregationMetadataUiState) {
        mutableUiState.update { state ->
            val newState = transform(state)
            newState.copy(permissions = congregationPermissions(permissionPolicy, currentUser, newState.editor))
        }
    }

    private companion object {
        const val MISSING_PERMISSION_MESSAGE = "Dafür fehlt dir die Berechtigung."
    }
}
