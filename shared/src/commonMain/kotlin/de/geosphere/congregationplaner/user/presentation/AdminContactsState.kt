package de.geosphere.congregationplaner.user.presentation

import de.geosphere.congregationplaner.user.domain.model.AdminContact

/** Ladezustand der Admins, die auf dem Bildschirm „Freigabe ausstehend“ stehen. */
sealed interface AdminContactsState {
    data object Loading : AdminContactsState

    data class Loaded(val contacts: List<AdminContact>) : AdminContactsState

    data object Failed : AdminContactsState
}

internal const val NO_ADMIN_TEXT = "kein Admin vorhanden"
internal const val UNKNOWN_NAME_TEXT = "Name nicht hinterlegt"

/** Admins, die sich selbst registriert haben, haben bis zur Pflege durch einen Admin keinen Namen. */
internal fun AdminContact.displayName(): String = fullName.ifBlank { UNKNOWN_NAME_TEXT }
