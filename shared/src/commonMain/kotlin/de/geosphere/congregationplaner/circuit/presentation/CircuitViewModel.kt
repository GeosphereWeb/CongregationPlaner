package de.geosphere.congregationplaner.circuit.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.circuit.domain.usecase.CreateCircuitUseCase
import de.geosphere.congregationplaner.circuit.domain.usecase.DeleteCircuitUseCase
import de.geosphere.congregationplaner.circuit.domain.usecase.ObserveCircuitsUseCase
import de.geosphere.congregationplaner.circuit.domain.usecase.UpdateCircuitUseCase
import de.geosphere.congregationplaner.presentation.components.ListDetailState
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.permission.CircuitPermissionPolicy
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Aus der [CircuitPermissionPolicy] abgeleitete UI-Berechtigungen; standardmäßig ist alles verboten. */
data class CircuitPermissions(
    val canCreate: Boolean = false,
    val canManage: Boolean = false,
    val canEditSelection: Boolean = false,
)

/**
 * Der gerade bearbeitete Kreis. Da die Kreisnummer fachlich vergeben wird, lässt sich ein neuer Kreis
 * nicht an einer leeren ID erkennen; [isNew] steuert daher Anlegen vs. Aktualisieren.
 */
data class CircuitEditorState(val circuit: Circuit, val isNew: Boolean, val original: Circuit = circuit) {
    val isDirty: Boolean get() = circuit != original
}

data class CircuitUiState(
    val circuits: List<Circuit> = emptyList(),
    val editor: CircuitEditorState? = null,
    val isLoading: Boolean = true,
    override val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    override val errorMessage: String? = null,
    val permissions: CircuitPermissions = CircuitPermissions(),
) : ListDetailState {
    override val hasDetail: Boolean get() = editor != null
}

@Inject
class CircuitViewModel(
    private val observeCircuits: ObserveCircuitsUseCase,
    private val createCircuit: CreateCircuitUseCase,
    private val updateCircuit: UpdateCircuitUseCase,
    private val deleteCircuitUseCase: DeleteCircuitUseCase,
    private val permissionPolicy: CircuitPermissionPolicy,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(CircuitUiState())
    val uiState = mutableUiState.asStateFlow()

    private var currentUser: AppUser? = null

    init {
        viewModelScope.launch {
            observeCircuits()
                .catch { exception ->
                    updateState {
                        it.copy(isLoading = false, errorMessage = exception.message ?: "Kreise konnten nicht geladen werden.")
                    }
                }.collect { circuits ->
                    updateState { it.copy(circuits = circuits, isLoading = false, errorMessage = null) }
                }
        }
    }

    fun onCurrentUserChanged(user: AppUser?) {
        currentUser = user
        updateState { it }
    }

    fun createCircuit() {
        val user = currentUser
        if (user == null || !permissionPolicy.canCreate(user)) {
            updateState { it.copy(errorMessage = MISSING_PERMISSION_MESSAGE) }
            return
        }
        updateState { it.copy(editor = CircuitEditorState(Circuit(), isNew = true), errorMessage = null) }
    }

    fun editCircuit(circuit: Circuit) {
        updateState { it.copy(editor = CircuitEditorState(circuit, isNew = false), errorMessage = null) }
    }

    fun updateEditor(circuit: Circuit) {
        val editor = mutableUiState.value.editor ?: return
        if (!mutableUiState.value.permissions.canEditSelection) return
        // Die Kreisnummer ist Teil des Firestore-Pfads und nach dem Anlegen unveränderlich.
        val updated = if (editor.isNew) circuit else circuit.copy(id = editor.circuit.id)
        updateState { it.copy(editor = editor.copy(circuit = updated), errorMessage = null) }
    }

    fun cancelEditing() {
        updateState { it.copy(editor = null, errorMessage = null) }
    }

    /** Verwirft Änderungen, lässt aber den ausgewählten Kreis geöffnet; ein neuer, ungespeicherter Kreis wird geschlossen. */
    fun discardChanges() {
        updateState { state ->
            val editor = state.editor?.takeUnless { it.isNew }?.let { it.copy(circuit = it.original) }
            state.copy(editor = editor, errorMessage = null)
        }
    }

    fun saveCircuit() {
        if (mutableUiState.value.isSaving || mutableUiState.value.isDeleting) return
        val editor = mutableUiState.value.editor ?: return
        val user = currentUser
        if (user == null || !canModify(user, editor)) {
            updateState { it.copy(errorMessage = MISSING_PERMISSION_MESSAGE) }
            return
        }

        updateState { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val result = if (editor.isNew) createCircuit(editor.circuit) else updateCircuit(editor.circuit)
            result.onSuccess { savedCircuit ->
                updateState { state ->
                    val circuits = if (state.circuits.any { it.id == savedCircuit.id }) {
                        state.circuits.map { current -> if (current.id == savedCircuit.id) savedCircuit else current }
                    } else {
                        (state.circuits + savedCircuit).sortedBy { it.id }
                    }
                    state.copy(
                        circuits = circuits,
                        editor = CircuitEditorState(savedCircuit, isNew = false),
                        isSaving = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { exception ->
                updateState {
                    it.copy(isSaving = false, errorMessage = exception.message ?: "Kreis konnte nicht gespeichert werden.")
                }
            }
        }
    }

    fun deleteCircuit(circuit: Circuit) {
        if (circuit.id.isBlank() || mutableUiState.value.isDeleting || mutableUiState.value.isSaving) return
        val user = currentUser
        if (user == null || !permissionPolicy.canDelete(user, circuit)) {
            updateState { it.copy(errorMessage = MISSING_PERMISSION_MESSAGE) }
            return
        }

        updateState { it.copy(isDeleting = true, errorMessage = null) }
        viewModelScope.launch {
            deleteCircuitUseCase(circuit.id)
                .onSuccess {
                    updateState {
                        it.copy(
                            circuits = it.circuits.filterNot { item -> item.id == circuit.id },
                            editor = it.editor?.takeUnless { editor -> !editor.isNew && editor.circuit.id == circuit.id },
                            isDeleting = false,
                            errorMessage = null,
                        )
                    }
                }.onFailure { exception ->
                    updateState {
                        it.copy(isDeleting = false, errorMessage = exception.message ?: "Kreis konnte nicht gelöscht werden.")
                    }
                }
        }
    }

    // Berechtigungen werden bei jeder Zustandsänderung neu abgeleitet, damit sie zu Nutzer und Auswahl passen.
    private fun updateState(transform: (CircuitUiState) -> CircuitUiState) {
        mutableUiState.update { state ->
            val newState = transform(state)
            newState.copy(permissions = permissionsFor(newState.editor))
        }
    }

    private fun permissionsFor(editor: CircuitEditorState?): CircuitPermissions {
        val user = currentUser ?: return CircuitPermissions()
        return CircuitPermissions(
            canCreate = permissionPolicy.canCreate(user),
            canManage = permissionPolicy.canManageGeneral(user),
            canEditSelection = editor?.let { canModify(user, it) } ?: false,
        )
    }

    private fun canModify(user: AppUser, editor: CircuitEditorState): Boolean = if (editor.isNew) {
        permissionPolicy.canCreate(user)
    } else {
        permissionPolicy.canEdit(user, editor.circuit)
    }

    private companion object {
        const val MISSING_PERMISSION_MESSAGE = "Dafür fehlt dir die Berechtigung."
    }
}
