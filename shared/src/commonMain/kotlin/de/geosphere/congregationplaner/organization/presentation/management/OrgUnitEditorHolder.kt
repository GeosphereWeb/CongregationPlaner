package de.geosphere.congregationplaner.organization.presentation.management

import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.usecase.LimitOrgUnitInputUseCase
import de.geosphere.congregationplaner.presentation.state.UiStateStore

/** Öffnet, ändert und schließt den Editor einer Organisationseinheit. */
class OrgUnitEditorHolder(
    private val store: UiStateStore<OrganizationUiState>,
    private val limitInput: LimitOrgUnitInputUseCase,
) {
    fun create() {
        if (store.value.canManage) store.update { it.copy(editor = OrgUnitEditorState(OrgUnit()), errorMessage = null) }
    }

    fun edit(unit: OrgUnit) = store.update { it.copy(editor = OrgUnitEditorState(unit), errorMessage = null) }

    /** Die ID ist Pfadsegment und bleibt unverändert; die Namen werden auf die erlaubte Länge gekürzt. */
    fun update(unit: OrgUnit) = store.update { state ->
        val editor = state.editor ?: return@update state
        state.copy(editor = editor.copy(unit = limitInput(unit, editor.original)))
    }

    fun cancel() = store.update { it.copy(editor = null, errorMessage = null) }

    /** Verwirft Änderungen, lässt aber die ausgewählte Einheit geöffnet; eine neue wird geschlossen. */
    fun discard() = store.update { state ->
        state.copy(
            editor = state.editor?.takeUnless {
                it.isNew
            }?.let { it.copy(unit = it.original) },
            errorMessage = null,
        )
    }
}
