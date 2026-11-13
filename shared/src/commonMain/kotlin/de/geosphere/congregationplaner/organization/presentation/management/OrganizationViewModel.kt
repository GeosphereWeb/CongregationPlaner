package de.geosphere.congregationplaner.organization.presentation.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.permission.OrganizationPermissionPolicy
import de.geosphere.congregationplaner.organization.domain.usecase.DeleteOrgUnitUseCase
import de.geosphere.congregationplaner.organization.domain.usecase.LimitOrgUnitInputUseCase
import de.geosphere.congregationplaner.organization.domain.usecase.ObserveOrgUnitsUseCase
import de.geosphere.congregationplaner.organization.domain.usecase.SaveOrgUnitUseCase
import de.geosphere.congregationplaner.presentation.components.ListDetailState
import de.geosphere.congregationplaner.presentation.state.UiStateStore
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Die gerade bearbeitete Einheit; eine neue hat noch keine ID. */
data class OrgUnitEditorState(val unit: OrgUnit, val original: OrgUnit = unit) {
    val isNew: Boolean get() = original.id.isBlank()
    val isDirty: Boolean get() = unit != original
}

/**
 * Der Bereich „Organisation“: [path] sind die aufgeklappten Einheiten von der Region abwärts, [units] die Einträge
 * der Ebene darunter. Ist ein Kreis aufgeklappt, ist die Ebene [OrgLevel.CONGREGATION]; die Versammlungen zeigt dann
 * die Versammlungsverwaltung.
 */
data class OrganizationUiState(
    val path: List<OrgUnit> = emptyList(),
    val units: List<OrgUnit> = emptyList(),
    val editor: OrgUnitEditorState? = null,
    val isLoading: Boolean = true,
    override val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    override val errorMessage: String? = null,
    val canManage: Boolean = false,
) : ListDetailState {
    val level: OrgLevel get() = OrgLevel.entries[path.size]
    val parentIds: List<String> get() = path.map(OrgUnit::id)
    override val hasDetail: Boolean get() = editor != null
}

@Inject
class OrganizationViewModel(
    private val observeUnits: ObserveOrgUnitsUseCase,
    private val saveUnit: SaveOrgUnitUseCase,
    private val deleteUnit: DeleteOrgUnitUseCase,
    private val policy: OrganizationPermissionPolicy,
    limitInput: LimitOrgUnitInputUseCase,
) : ViewModel() {
    private val store = UiStateStore(OrganizationUiState())
    val uiState: StateFlow<OrganizationUiState> = store.state

    /** Anlegen und Bearbeiten einer Einheit (ADR-0025). */
    val editor = OrgUnitEditorHolder(store, limitInput)

    private var currentUser: AppUser? = null
    private var observeJob: Job? = null

    fun onCurrentUserChanged(user: AppUser) {
        val first = currentUser == null
        currentUser = user
        store.update { it.copy(canManage = policy.canManage(user)) }
        if (first) observe()
    }

    /** Klappt [unit] der aktuellen Ebene auf und zeigt die Ebene darunter. */
    fun open(unit: OrgUnit) {
        if (unit.id.isBlank() || !store.value.level.isOrgUnit) return
        navigate(store.value.path + unit)
    }

    /** Springt zurück auf die Ebene mit [depth] aufgeklappten Einheiten (0 = Regionen). */
    fun goTo(depth: Int) {
        val path = store.value.path
        if (depth in 0 until path.size) navigate(path.take(depth))
    }

    /** Eine Ebene nach oben; `false`, wenn schon die Regionen zu sehen sind. */
    fun back(): Boolean {
        val depth = store.value.path.size
        if (depth == 0) return false
        goTo(depth - 1)
        return true
    }

    fun save() {
        val state = store.value
        val editor = state.editor ?: return
        val user = currentUser ?: return
        if (state.isSaving || state.isDeleting) return
        store.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            saveUnit(user, state.level, state.parentIds, editor.unit)
                .onSuccess { saved ->
                    store.update {
                        it.copy(
                            units = (it.units.filterNot { unit -> unit.id == saved.id } + saved).sortedForList(),
                            editor = OrgUnitEditorState(saved),
                            isSaving = false,
                        )
                    }
                }.onFailure { error ->
                    store.update {
                        it.copy(isSaving = false, errorMessage = error.message ?: "Speichern fehlgeschlagen.")
                    }
                }
        }
    }

    fun delete(unit: OrgUnit) {
        val state = store.value
        val user = currentUser ?: return
        if (state.isSaving || state.isDeleting) return
        store.update { it.copy(isDeleting = true, errorMessage = null) }
        viewModelScope.launch {
            deleteUnit(user, state.level, state.parentIds, unit)
                .onSuccess {
                    store.update {
                        it.copy(
                            units = it.units.filterNot { item -> item.id == unit.id },
                            editor = it.editor?.takeUnless { editor -> editor.original.id == unit.id },
                            isDeleting = false,
                        )
                    }
                }.onFailure { error ->
                    store.update {
                        it.copy(isDeleting = false, errorMessage = error.message ?: "Löschen fehlgeschlagen.")
                    }
                }
        }
    }

    private fun navigate(path: List<OrgUnit>) {
        store.update { it.copy(path = path, units = emptyList(), editor = null, errorMessage = null) }
        observe()
    }

    private fun observe() {
        observeJob?.cancel()
        val state = store.value
        if (!state.level.isOrgUnit) {
            store.update { it.copy(isLoading = false) }
            return
        }
        store.update { it.copy(isLoading = true) }
        observeJob = viewModelScope.launch {
            observeUnits(state.level, state.parentIds)
                .catch { error ->
                    store.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Laden fehlgeschlagen.")
                    }
                }.collect { units -> store.update { it.copy(units = units, isLoading = false) } }
        }
    }
}

internal fun List<OrgUnit>.sortedForList(): List<OrgUnit> = sortedBy { it.displayName.lowercase() }
