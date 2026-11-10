package de.geosphere.congregationplaner.organization.presentation.selection

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.SelectedCongregation
import de.geosphere.congregationplaner.organization.domain.usecase.LoadOrganizationUnitsUseCase
import de.geosphere.congregationplaner.presentation.state.UiStateStore

/**
 * Wählt die Versammlung über einen Einladungslink. [launchLoad] ersetzt ein laufendes Laden, damit die Auswahl im
 * Baum und der Link sich nicht überholen.
 */
class InviteLinkHolder(
    private val store: UiStateStore<CongregationSelectionUiState>,
    private val loadUnits: LoadOrganizationUnitsUseCase,
    private val launchLoad: (suspend () -> Unit) -> Unit,
    private val onResolved: (SelectedCongregation) -> Unit,
) {
    fun change(input: String) = store.update { it.copy(inviteLinkInput = input, errorMessage = null) }

    /** Übernimmt einen Einladungslink aus der Zwischenablage; ohne Text gibt es eine Fehlermeldung. */
    fun paste(clipboardText: String?) {
        if (clipboardText.isNullOrBlank()) {
            store.update { it.copy(errorMessage = EMPTY_CLIPBOARD_MESSAGE) }
        } else {
            change(clipboardText)
            applyLink(clipboardText)
        }
    }

    /** Übernimmt die Versammlung aus einem Einladungslink, nachdem geprüft wurde, dass es sie gibt. */
    fun applyLink(link: String = store.value.inviteLinkInput) {
        val ref = CongregationRef.parse(link)
        if (ref == null) {
            store.update { it.copy(errorMessage = INVALID_LINK_MESSAGE) }
            return
        }
        store.update { it.copy(isResolvingLink = true, errorMessage = null) }
        launchLoad {
            loadUnits(OrgLevel.CONGREGATION, ref.ids.dropLast(1))
                .onSuccess { congregations ->
                    val congregation = congregations.firstOrNull { it.id == ref.congregationId }
                    if (congregation == null) {
                        store.update { it.copy(isResolvingLink = false, errorMessage = UNKNOWN_LINK_MESSAGE) }
                    } else {
                        val circuitName = loadUnits(OrgLevel.CIRCUIT, ref.ids.dropLast(2)).getOrNull()
                            ?.firstOrNull { it.id == ref.circuitId }?.name.orEmpty()
                        onResolved(SelectedCongregation(ref, congregation.name, circuitName))
                    }
                }.onFailure { exception ->
                    store.update {
                        it.copy(isResolvingLink = false, errorMessage = exception.message ?: UNKNOWN_LINK_MESSAGE)
                    }
                }
        }
    }

    private companion object {
        const val INVALID_LINK_MESSAGE = "Der Einladungslink ist ungültig."
        const val EMPTY_CLIPBOARD_MESSAGE = "Die Zwischenablage enthält keinen Text."
        const val UNKNOWN_LINK_MESSAGE = "Die Versammlung aus dem Einladungslink wurde nicht gefunden."
    }
}
