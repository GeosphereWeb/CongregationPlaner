package de.geosphere.congregationplaner.organization.presentation.selection

import de.geosphere.congregationplaner.auth.domain.repository.FirebaseUser
import de.geosphere.congregationplaner.congregation.domain.usecase.CreateOwnCongregationUseCase
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.SelectedCongregation
import de.geosphere.congregationplaner.presentation.state.UiStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Eine neue Versammlung im gewählten Kreis: Name eingeben, bestätigen und nach der Anmeldung anlegen (ADR-0018). */
class NewCongregationHolder(
    private val store: UiStateStore<CongregationSelectionUiState>,
    private val scope: CoroutineScope,
    private val createOwnCongregation: CreateOwnCongregationUseCase,
    private val onCreated: (SelectedCongregation) -> Unit,
) {
    /** Beginnt eine neue Versammlung im gewählten Kreis; den Namen gibt der Nutzer danach ein. */
    fun start() {
        val levels = store.value.levels
        val circuit = levels.getOrNull(OrgLevel.CIRCUIT.ordinal)?.takeIf { it.selectedId != null } ?: return
        val circuitIds = levels.take(OrgLevel.CIRCUIT.ordinal + 1).mapNotNull { it.selectedId }
        store.update {
            it.copy(
                newCongregation = NewCongregationDraft(circuitIds, circuit.selectedUnit?.name.orEmpty()),
                errorMessage = null,
            )
        }
    }

    fun changeName(name: String) = store.update { state ->
        state.copy(
            newCongregation = state.newCongregation?.copy(name = createOwnCongregation.limitName(name)),
            errorMessage = null,
        )
    }

    fun confirm() {
        if (!store.value.isNewCongregationReady) return
        store.update { state -> state.copy(newCongregation = state.newCongregation?.copy(confirmed = true)) }
    }

    /** Zurück zur Namenseingabe, z. B. um den vorgemerkten Namen zu ändern. */
    fun edit() = store.update { state -> state.copy(newCongregation = state.newCongregation?.copy(confirmed = false)) }

    fun cancel() = store.update { it.copy(newCongregation = null, isCreating = false, errorMessage = null) }

    /**
     * Legt die vorgemerkte Versammlung an und wählt sie; [user] wird ihr erster Admin. Das geht erst nach der
     * Anmeldung mit bestätigter E-Mail-Adresse.
     */
    fun create(user: FirebaseUser) {
        val draft = store.value.newCongregation
        if (draft == null || store.value.isCreating) return
        store.update { it.copy(isCreating = true, errorMessage = null) }
        scope.launch {
            createOwnCongregation(draft.circuitIds, draft.name, user)
                .onSuccess { created ->
                    val ref = CongregationRef.fromIds(draft.circuitIds + created.id)
                    if (ref == null) {
                        store.update { it.copy(isCreating = false, errorMessage = CREATED_WITHOUT_PATH_MESSAGE) }
                    } else {
                        onCreated(SelectedCongregation(ref, created.name, draft.circuitName))
                    }
                }.onFailure { exception ->
                    val message = exception.message ?: "Die Versammlung konnte nicht angelegt werden."
                    store.update { it.copy(isCreating = false, errorMessage = message) }
                }
        }
    }

    private companion object {
        const val CREATED_WITHOUT_PATH_MESSAGE = "Die Versammlung wurde angelegt, konnte aber nicht gewählt werden."
    }
}
