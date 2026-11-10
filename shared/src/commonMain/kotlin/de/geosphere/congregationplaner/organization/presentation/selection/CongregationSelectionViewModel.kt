package de.geosphere.congregationplaner.organization.presentation.selection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.congregation.domain.model.CongregationName
import de.geosphere.congregationplaner.congregation.domain.usecase.CreateOwnCongregationUseCase
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.model.SelectedCongregation
import de.geosphere.congregationplaner.organization.domain.repository.CongregationSelectionStore
import de.geosphere.congregationplaner.organization.domain.usecase.LoadOrganizationUnitsUseCase
import de.geosphere.congregationplaner.presentation.state.UiStateStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
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

/** Eine neue Versammlung, die der Nutzer anlegen will: der Kreis steht fest, der Name wird eingegeben. */
data class NewCongregationDraft(
    val circuitIds: List<String>,
    val circuitName: String,
    val name: String = "",
    val confirmed: Boolean = false,
)

data class CongregationSelectionUiState(
    val selected: SelectedCongregation? = null,
    /** Die Ebenen, die gerade ausgewählt werden; leer, solange nicht gewählt wird. */
    val levels: List<OrgLevelSelection> = emptyList(),
    val inviteLinkInput: String = "",
    val isResolvingLink: Boolean = false,
    val errorMessage: String? = null,
    val newCongregation: NewCongregationDraft? = null,
    val isCreating: Boolean = false,
) {
    val isPicking: Boolean get() = levels.isNotEmpty()

    val canApplyInviteLink: Boolean get() = inviteLinkInput.isNotBlank() && !isResolvingLink

    /**
     * Die Versammlung, mit der die Anmeldung läuft: solange eine neue vorgemerkt ist, zählt die gespeicherte Auswahl
     * nicht, sonst würde der Nutzer deren Mitglied statt Admin der neuen.
     */
    val activeSelection: SelectedCongregation? get() = if (newCongregation != null) null else selected

    /** Eine neue Versammlung lässt sich anlegen, sobald der Kreis gewählt ist. */
    val canStartNewCongregation: Boolean
        get() = levels.getOrNull(OrgLevel.CIRCUIT.ordinal)?.selectedId != null

    /** Kreis und gültiger Name stehen fest; angelegt wird nach der Anmeldung. */
    val isNewCongregationReady: Boolean
        get() = newCongregation?.let { CongregationName.validate(it.name) == null } == true

    /** Der Nutzer hat den gültigen Namen mit „Weiter“ bestätigt; erst dann folgt die Anmeldung. */
    val isNewCongregationConfirmed: Boolean
        get() = isNewCongregationReady && newCongregation?.confirmed == true
}

/**
 * Lässt den Nutzer vor der Anmeldung seine Versammlung wählen: Ebene für Ebene von der Region bis zur Versammlung
 * oder über einen Einladungslink. Die Auswahl wird lokal gespeichert. Neue Versammlung und Einladungslink übernehmen
 * die Zustandshalter (ADR-0025).
 */
@Inject
class CongregationSelectionViewModel(
    private val loadUnits: LoadOrganizationUnitsUseCase,
    private val store: CongregationSelectionStore,
    createOwnCongregation: CreateOwnCongregationUseCase,
) : ViewModel() {
    private val uiStore = UiStateStore(CongregationSelectionUiState(selected = store.load()))
    val uiState: StateFlow<CongregationSelectionUiState> = uiStore.state

    private var loadJob: Job? = null

    val newCongregation = NewCongregationHolder(uiStore, viewModelScope, createOwnCongregation, ::select)
    val inviteLink = InviteLinkHolder(uiStore, loadUnits, ::launchLoad, ::select)

    init {
        if (uiStore.value.selected == null) startPicking()
    }

    fun startPicking() {
        uiStore.update {
            it.copy(levels = listOf(OrgLevelSelection(OrgLevel.REGION, isLoading = true)), errorMessage = null)
        }
        loadLevel(OrgLevel.REGION, emptyList())
    }

    /** Bricht die Auswahl ab; nur möglich, wenn bereits eine Versammlung gewählt ist. */
    fun cancelPicking() {
        if (uiStore.value.selected == null) return
        loadJob?.cancel()
        uiStore.update { it.copy(levels = emptyList(), errorMessage = null) }
    }

    fun selectUnit(level: OrgLevel, unitId: String) {
        val levels = uiStore.value.levels
        val current = levels.getOrNull(level.ordinal)?.takeIf { it.level == level } ?: return
        val unit = current.options.firstOrNull { it.id == unitId } ?: return
        // Tiefere Ebenen hängen von dieser Auswahl ab und werden verworfen.
        val updatedLevels = levels.take(level.ordinal) + current.copy(selectedId = unit.id)
        val ids = updatedLevels.mapNotNull { it.selectedId }
        val child = level.child
        if (child == null) {
            val circuitName = updatedLevels.firstOrNull { it.level == OrgLevel.CIRCUIT }?.selectedUnit?.name.orEmpty()
            CongregationRef.fromIds(ids)?.let { select(SelectedCongregation(it, unit.name, circuitName)) }
        } else {
            uiStore.update {
                it.copy(levels = updatedLevels + OrgLevelSelection(child, isLoading = true), errorMessage = null)
            }
            loadLevel(child, ids)
        }
    }

    /** Lädt die zuletzt gewählte Ebene erneut, z. B. nachdem das Netzwerk wieder verfügbar ist. */
    fun retry() {
        val levels = uiStore.value.levels
        val last = levels.lastOrNull() ?: return startPicking()
        uiStore.update { it.copy(levels = levels.dropLast(1) + last.copy(isLoading = true), errorMessage = null) }
        loadLevel(last.level, levels.dropLast(1).mapNotNull { it.selectedId })
    }

    /** Verwirft die gespeicherte Versammlung, z. B. um eine andere zu wählen. */
    fun clearSelection() {
        store.clear()
        uiStore.update { it.copy(selected = null) }
        startPicking()
    }

    private fun select(selection: SelectedCongregation) {
        store.save(selection)
        uiStore.update {
            it.copy(
                selected = selection,
                levels = emptyList(),
                inviteLinkInput = "",
                isResolvingLink = false,
                newCongregation = null,
                isCreating = false,
                errorMessage = null,
            )
        }
    }

    private fun launchLoad(block: suspend () -> Unit) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { block() }
    }

    private fun loadLevel(level: OrgLevel, parentIds: List<String>) = launchLoad {
        val result = loadUnits(level, parentIds)
        uiStore.update { state -> state.withLoadedLevel(level, parentIds, result) }
    }
}

/** Übernimmt das Laden einer Ebene; Ergebnisse einer inzwischen geänderten Auswahl werden verworfen. */
internal fun CongregationSelectionUiState.withLoadedLevel(
    level: OrgLevel,
    parentIds: List<String>,
    result: Result<List<OrgUnit>>,
): CongregationSelectionUiState {
    val index = level.ordinal
    val entry = levels.getOrNull(index)
    if (entry?.level != level || levels.take(index).mapNotNull { it.selectedId } != parentIds) return this
    return result.fold(
        onSuccess = { units ->
            copy(
                levels = levels.take(index) + entry.copy(options = units, isLoading = false),
                errorMessage = if (units.isEmpty()) "Keine Einträge für ${level.label} gefunden." else null,
            )
        },
        onFailure = { exception ->
            copy(
                levels = levels.take(index) + entry.copy(isLoading = false),
                errorMessage = exception.message ?: "${level.label} konnte nicht geladen werden.",
            )
        },
    )
}
