package de.geosphere.congregationplaner.user.presentation.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.presentation.state.UiStateStore
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.usecase.ObserveMembersUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Verwaltung der Mitglieder der Versammlung des angemeldeten Versammlungsadmins. Das ViewModel beobachtet die
 * Mitglieder; Zugang und Profil-Editor übernehmen die Zustandshalter [access] und [editor] (ADR-0025).
 */
@Inject
class UserManagementViewModel(
    private val observeMembers: ObserveMembersUseCase,
    accessUseCases: MemberAccessUseCases,
    editorUseCases: MemberEditorUseCases,
    private val permissionPolicy: UserPermissionPolicy,
) : ViewModel() {
    private var currentUser: AppUser? = null

    // Die Rechte werden bei jeder Zustandsänderung neu abgeleitet, damit sie zu Nutzer und Mitgliederliste passen.
    private val store = UiStateStore(UserManagementUiState()) {
        it.copy(memberPermissions = memberPermissions(permissionPolicy, currentUser, it.members))
    }
    val uiState: StateFlow<UserManagementUiState> = store.state

    val access = MemberAccessHolder(store, viewModelScope, accessUseCases) { currentUser }
    val editor = MemberEditorHolder(store, viewModelScope, editorUseCases) { currentUser }

    private val inviteLinks = InviteLinkCache()
    private var observeJob: Job? = null
    private var observedCongregation: CongregationRef? = null
    private var observing = false

    fun onCurrentUserChanged(user: AppUser) {
        currentUser = user
        val canManage = permissionPolicy.canManageUsers(user)
        // Mitglieder werden nur beobachtet, solange der Nutzer sie verwalten darf.
        val congregation = user.congregation?.takeIf { canManage }
        store.update {
            it.copy(
                currentUserId = user.uid,
                inviteLink = inviteLinks.linkFor(congregation),
                canManage = canManage,
                editor = it.editor?.takeIf { canManage },
            )
        }
        if (!observing || congregation != observedCongregation) observe(congregation)
    }

    fun dismissError() = store.update { it.copy(errorMessage = null) }

    private fun observe(congregation: CongregationRef?) {
        observeJob?.cancel()
        observedCongregation = congregation
        observing = true
        if (congregation == null) {
            store.update { it.copy(members = emptyList(), isLoading = false) }
            return
        }
        store.update { it.copy(isLoading = true) }
        observeJob = viewModelScope.launch {
            observeMembers(congregation)
                .catch { exception ->
                    val message = exception.message ?: "Mitglieder konnten nicht geladen werden."
                    store.update { it.copy(isLoading = false, errorMessage = message) }
                }.collect { members ->
                    store.update { it.copy(members = members, isLoading = false) }
                }
        }
    }
}

/** Der Link ist bei jedem Verschlüsseln ein anderer; er wird je Versammlung gemerkt, damit nichts springt. */
internal class InviteLinkCache {
    private var congregation: CongregationRef? = null
    private var link: String? = null

    fun linkFor(target: CongregationRef?): String? {
        if (target != congregation) {
            congregation = target
            link = target?.toInviteLink()
        }
        return link
    }
}
