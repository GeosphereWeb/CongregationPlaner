package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.presentation.state.UiStateStore
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.user.domain.model.AppUser
import kotlinx.coroutines.CoroutineScope

/** Was alle Halter des Wochenplans teilen: Zustand, Scope, angemeldeter Nutzer und die Prüfung der Rechte. */
class TalkScheduleSession(
    val store: UiStateStore<TalkScheduleUiState>,
    val scope: CoroutineScope,
    private val policy: TalkSchedulePermissionPolicy,
) {
    var user: AppUser? = null

    /**
     * Der Nutzer, sofern er planen darf; mit [confirmOnly] genügt das Recht des Helfers (Termin öffnen, „Informiert“
     * speichern). Sonst meldet die Funktion die fehlende Berechtigung und liefert `null`.
     */
    fun allowedUser(confirmOnly: Boolean = false): AppUser? {
        val permitted = if (confirmOnly) policy::canConfirmSpeaker else policy::canPlanTalks
        val allowed = user?.takeIf { permitted(it) && it.congregation != null }
        if (allowed == null) store.update { it.copy(errorMessage = MISSING_PERMISSION_MESSAGE) }
        return allowed
    }

    fun fail(exception: Throwable, fallback: String) =
        store.update { it.copy(isSaving = false, errorMessage = exception.message ?: fallback) }

    private companion object {
        const val MISSING_PERMISSION_MESSAGE = "Dafür fehlt dir die Berechtigung."
    }
}
