package de.geosphere.congregationplaner.user.presentation.management

import de.geosphere.congregationplaner.presentation.state.UiStateStore
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.usecase.RemoveMemberUseCase
import de.geosphere.congregationplaner.user.domain.usecase.SetMemberApprovalUseCase
import de.geosphere.congregationplaner.user.domain.usecase.SetMemberRoleUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Die UseCases für den Zugang eines Mitglieds. */
@Inject
class MemberAccessUseCases(
    val setMemberApproval: SetMemberApprovalUseCase,
    val setMemberRole: SetMemberRoleUseCase,
    val removeMember: RemoveMemberUseCase,
)

/** Freigabe, Rollen und Entfernen eines Mitglieds; jede Änderung wirkt sofort. */
class MemberAccessHolder(
    private val store: UiStateStore<UserManagementUiState>,
    private val scope: CoroutineScope,
    private val useCases: MemberAccessUseCases,
    private val currentUser: () -> AppUser?,
) {
    fun setApproved(member: AppUser, approved: Boolean) = change(member, "Mitglied konnte nicht gespeichert werden.") {
        useCases.setMemberApproval(it, member, approved).map { saved -> { state -> state.withSavedMember(saved) } }
    }

    fun toggleRole(member: AppUser, role: CongregationRole) =
        change(member, "Mitglied konnte nicht gespeichert werden.") {
            val members = store.value.members
            useCases.setMemberRole(it, member, role, granted = role !in member.roles, members = members)
                .map { saved -> { state -> state.withSavedMember(saved) } }
        }

    fun remove(member: AppUser) = change(member, "Mitglied konnte nicht entfernt werden.") {
        useCases.removeMember(it, member).map {
            { state ->
                state.copy(
                    members = state.members.filterNot { listed -> listed.uid == member.uid },
                    editor = state.editor?.takeUnless { editor -> editor.member?.uid == member.uid },
                    busyMemberIds = state.busyMemberIds - member.uid,
                )
            }
        }
    }

    /**
     * Führt [action] für [member] aus, sofern für ihn nichts anderes läuft; das Ergebnis liefert die Änderung am
     * Zustand, ein Fehler die Meldung (sonst [fallback]).
     */
    private fun change(
        member: AppUser,
        fallback: String,
        action: suspend (AppUser) -> Result<(UserManagementUiState) -> UserManagementUiState>,
    ) {
        val user = currentUser()
        if (user == null || member.uid in store.value.busyMemberIds) return
        store.update { it.copy(busyMemberIds = it.busyMemberIds + member.uid, errorMessage = null) }
        scope.launch {
            val result = action(user)
            store.update { state ->
                result.fold(
                    onSuccess = { apply -> apply(state) },
                    onFailure = { exception ->
                        state.copy(
                            busyMemberIds = state.busyMemberIds - member.uid,
                            errorMessage = exception.message ?: fallback,
                        )
                    },
                )
            }
        }
    }
}
