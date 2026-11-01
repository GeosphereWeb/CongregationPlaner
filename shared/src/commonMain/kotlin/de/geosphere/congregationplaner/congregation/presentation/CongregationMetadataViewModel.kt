package de.geosphere.congregationplaner.congregation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.usecase.DeleteCongregationUseCase
import de.geosphere.congregationplaner.congregation.domain.usecase.ObserveCongregationsUseCase
import de.geosphere.congregationplaner.congregation.domain.usecase.SaveCongregationUseCase
import de.geosphere.congregationplaner.presentation.components.ListDetailState
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.permission.CongregationPermissionPolicy
import dev.zacsweers.metro.Inject
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
}

@Inject
class CongregationMetadataViewModel(
    private val observeCongregations: ObserveCongregationsUseCase,
    private val saveCongregation: SaveCongregationUseCase,
    private val deleteCongregationUseCase: DeleteCongregationUseCase,
    private val permissionPolicy: CongregationPermissionPolicy,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(CongregationMetadataUiState())
    val uiState = mutableUiState.asStateFlow()

    private var currentUser: AppUser? = null
    private var allCongregations: List<CongregationMetadata> = emptyList()
    private var ownCongregationOnly = false

    init {
        viewModelScope.launch {
            observeCongregations()
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
                        it.copy(congregations = visibleCongregations(), isLoading = false, errorMessage = null)
                    }
                }
        }
    }

    /**
     * Mit [ownOnly] zeigt die Liste nur die eigene Versammlung des Nutzers (Bereich „Versammlung“); sonst alle
     * (Globale Verwaltung).
     */
    fun onCurrentUserChanged(user: AppUser?, ownOnly: Boolean = false) {
        currentUser = user
        ownCongregationOnly = ownOnly
        updateState { it.copy(congregations = visibleCongregations()) }
    }

    private fun visibleCongregations(): List<CongregationMetadata> {
        if (!ownCongregationOnly) return allCongregations
        val ownId = currentUser?.congregation?.congregationId
        return allCongregations.filter { it.id == ownId }
    }

    fun createCongregation() {
        val user = currentUser
        if (user == null || !permissionPolicy.canCreate(user)) {
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

    /** Verwirft Änderungen, lässt aber die ausgewählte Versammlung geöffnet; eine neue, ungespeicherte wird geschlossen. */
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
        if (user == null || !canModify(user, congregation)) {
            updateState { it.copy(errorMessage = MISSING_PERMISSION_MESSAGE) }
            return
        }
        if (congregation.name.isBlank()) {
            updateState { it.copy(errorMessage = "Bitte gib einen Versammlungsnamen ein.") }
            return
        }

        updateState { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            saveCongregation(congregation.copy(name = congregation.name.trim()))
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
        if (user == null || !permissionPolicy.canDelete(user, congregation)) {
            updateState { it.copy(errorMessage = MISSING_PERMISSION_MESSAGE) }
            return
        }

        updateState { it.copy(isDeleting = true, errorMessage = null) }
        viewModelScope.launch {
            deleteCongregationUseCase(congregation.id)
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
            newState.copy(permissions = permissionsFor(newState.editor))
        }
    }

    private fun permissionsFor(editor: CongregationMetadata?): CongregationPermissions {
        val user = currentUser ?: return CongregationPermissions()
        return CongregationPermissions(
            canCreate = permissionPolicy.canCreate(user),
            canManage = permissionPolicy.canManageGeneral(user),
            canEditSelection = editor?.let { canModify(user, it) } ?: false,
            canEditMeetingTimes = editor?.let { canModifyMeetingTimes(user, it) } ?: false,
        )
    }

    // Eine neue Versammlung legt der globale Admin samt Zeiten an; danach ändert nur der Versammlungsadmin die Zeiten.
    private fun canModifyMeetingTimes(user: AppUser, congregation: CongregationMetadata): Boolean =
        if (congregation.id.isBlank()) {
            permissionPolicy.canCreate(user)
        } else {
            permissionPolicy.canEditMeetingTimes(user, congregation)
        }

    private fun canModify(user: AppUser, congregation: CongregationMetadata): Boolean = if (congregation.id.isBlank()) {
        permissionPolicy.canCreate(user)
    } else {
        permissionPolicy.canEdit(user, congregation)
    }

    private companion object {
        const val MISSING_PERMISSION_MESSAGE = "Dafür fehlt dir die Berechtigung."
    }
}
