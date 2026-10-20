package de.geosphere.congregationplaner.organization.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.model.SelectedCongregation
import de.geosphere.congregationplaner.organization.domain.repository.CongregationSelectionStore
import de.geosphere.congregationplaner.organization.domain.usecase.LoadOrganizationUnitsUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Auswahl auf einer Ebene der Organisationsstruktur. */
data class OrgLevelSelection(
    val level: OrgLevel,
    val options: List<OrgUnit> = emptyList(),
    val selectedId: String? = null,
    val isLoading: Boolean = false,
) {
    val selectedUnit: OrgUnit? get() = options.firstOrNull { it.id == selectedId }
}

data class CongregationSelectionUiState(
    val selected: SelectedCongregation? = null,
    /** Die Ebenen, die gerade ausgewählt werden; leer, solange nicht gewählt wird. */
    val levels: List<OrgLevelSelection> = emptyList(),
    val inviteLinkInput: String = "",
    val isResolvingLink: Boolean = false,
    val errorMessage: String? = null,
) {
    val isPicking: Boolean get() = levels.isNotEmpty()
}

/**
 * Lässt den Nutzer vor der Anmeldung seine Versammlung wählen: Ebene für Ebene von der Region bis zur
 * Versammlung oder über einen Einladungslink. Die Auswahl wird lokal gespeichert.
 */
@Inject
class CongregationSelectionViewModel(
    private val loadUnits: LoadOrganizationUnitsUseCase,
    private val store: CongregationSelectionStore,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(CongregationSelectionUiState(selected = store.load()))
    val uiState = mutableUiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        if (mutableUiState.value.selected == null) startPicking()
    }

    fun startPicking() {
        mutableUiState.update {
            it.copy(levels = listOf(OrgLevelSelection(OrgLevel.REGION, isLoading = true)), errorMessage = null)
        }
        loadLevel(OrgLevel.REGION, emptyList())
    }

    /** Bricht die Auswahl ab; nur möglich, wenn bereits eine Versammlung gewählt ist. */
    fun cancelPicking() {
        if (mutableUiState.value.selected == null) return
        loadJob?.cancel()
        mutableUiState.update { it.copy(levels = emptyList(), errorMessage = null) }
    }

    fun selectUnit(level: OrgLevel, unitId: String) {
        val levels = mutableUiState.value.levels
        val current = levels.getOrNull(level.ordinal)?.takeIf { it.level == level } ?: return
        val unit = current.options.firstOrNull { it.id == unitId } ?: return
        // Tiefere Ebenen hängen von dieser Auswahl ab und werden verworfen.
        val updatedLevels = levels.take(level.ordinal) + current.copy(selectedId = unit.id)
        val ids = updatedLevels.mapNotNull { it.selectedId }

        val child = level.child
        if (child == null) {
            val ref = CongregationRef.fromIds(ids) ?: return
            select(SelectedCongregation(ref, unit.name))
            return
        }
        mutableUiState.update {
            it.copy(levels = updatedLevels + OrgLevelSelection(child, isLoading = true), errorMessage = null)
        }
        loadLevel(child, ids)
    }

    fun onInviteLinkChanged(input: String) {
        mutableUiState.update { it.copy(inviteLinkInput = input, errorMessage = null) }
    }

    /** Übernimmt die Versammlung aus einem Einladungslink, nachdem geprüft wurde, dass es sie gibt. */
    fun applyInviteLink(link: String = mutableUiState.value.inviteLinkInput) {
        val ref = CongregationRef.parse(link)
        if (ref == null) {
            mutableUiState.update { it.copy(errorMessage = INVALID_LINK_MESSAGE) }
            return
        }
        loadJob?.cancel()
        mutableUiState.update { it.copy(isResolvingLink = true, errorMessage = null) }
        loadJob = viewModelScope.launch {
            loadUnits(OrgLevel.CONGREGATION, ref.ids.dropLast(1))
                .onSuccess { congregations ->
                    val congregation = congregations.firstOrNull { it.id == ref.congregationId }
                    if (congregation == null) {
                        mutableUiState.update { it.copy(isResolvingLink = false, errorMessage = UNKNOWN_LINK_MESSAGE) }
                    } else {
                        select(SelectedCongregation(ref, congregation.name))
                    }
                }.onFailure { exception ->
                    mutableUiState.update {
                        it.copy(isResolvingLink = false, errorMessage = exception.message ?: UNKNOWN_LINK_MESSAGE)
                    }
                }
        }
    }

    /** Lädt die zuletzt gewählte Ebene erneut, z. B. nachdem das Netzwerk wieder verfügbar ist. */
    fun retry() {
        val levels = mutableUiState.value.levels
        val last = levels.lastOrNull() ?: return startPicking()
        mutableUiState.update {
            it.copy(levels = levels.dropLast(1) + last.copy(isLoading = true), errorMessage = null)
        }
        loadLevel(last.level, levels.dropLast(1).mapNotNull { it.selectedId })
    }

    /** Verwirft die gespeicherte Versammlung, z. B. um eine andere zu wählen. */
    fun clearSelection() {
        store.clear()
        mutableUiState.update { it.copy(selected = null) }
        startPicking()
    }

    private fun select(selection: SelectedCongregation) {
        store.save(selection)
        mutableUiState.update {
            it.copy(
                selected = selection,
                levels = emptyList(),
                inviteLinkInput = "",
                isResolvingLink = false,
                errorMessage = null,
            )
        }
    }

    private fun loadLevel(level: OrgLevel, parentIds: List<String>) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val result = loadUnits(level, parentIds)
            mutableUiState.update { state ->
                val index = level.ordinal
                val entry = state.levels.getOrNull(index)
                // Ergebnisse einer inzwischen geänderten Auswahl werden verworfen.
                if (entry?.level != level || state.levels.take(index).mapNotNull { it.selectedId } != parentIds) {
                    return@update state
                }
                result.fold(
                    onSuccess = { units ->
                        val message = if (units.isEmpty()) "Keine Einträge für ${level.label} gefunden." else null
                        state.copy(
                            levels = state.levels.take(index) + entry.copy(options = units, isLoading = false),
                            errorMessage = message,
                        )
                    },
                    onFailure = { exception ->
                        state.copy(
                            levels = state.levels.take(index) + entry.copy(isLoading = false),
                            errorMessage = exception.message ?: "${level.label} konnte nicht geladen werden.",
                        )
                    },
                )
            }
        }
    }

    private companion object {
        const val INVALID_LINK_MESSAGE = "Der Einladungslink ist ungültig."
        const val UNKNOWN_LINK_MESSAGE = "Die Versammlung aus dem Einladungslink wurde nicht gefunden."
    }
}
