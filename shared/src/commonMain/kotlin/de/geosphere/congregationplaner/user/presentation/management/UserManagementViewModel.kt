package de.geosphere.congregationplaner.user.presentation.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.presentation.components.ListDetailState
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.permission.AdminRoleLock
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.usecase.CreateMemberWithoutAccountUseCase
import de.geosphere.congregationplaner.user.domain.usecase.ObserveMembersUseCase
import de.geosphere.congregationplaner.user.domain.usecase.RemoveMemberUseCase
import de.geosphere.congregationplaner.user.domain.usecase.SetMemberApprovalUseCase
import de.geosphere.congregationplaner.user.domain.usecase.SetMemberRoleUseCase
import de.geosphere.congregationplaner.user.domain.usecase.UpdateMemberProfileUseCase
import de.geosphere.congregationplaner.user.presentation.contact.withinInputLimits
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Die gerade bearbeiteten persönlichen Angaben; [member] ist `null`, solange eine neue Person ohne Konto entsteht. */
data class MemberEditorState(
    val profile: MemberProfile = MemberProfile(),
    val member: AppUser? = null,
    val original: MemberProfile = profile,
) {
    val isNew: Boolean get() = member == null
    val isDirty: Boolean get() = profile != original
}

/** Was der angemeldete Nutzer mit einem Mitglied tun darf; standardmäßig nichts. */
data class MemberPermissions(
    /** Freigeben und entfernen. */
    val canEdit: Boolean = false,
    val canEditProfile: Boolean = false,
    val toggleableRoles: Set<CongregationRole> = emptySet(),
    /** Warum die Admin-Rolle bestehen bleibt, sonst `null`. */
    val adminRoleLockReason: String? = null,
)

data class UserManagementUiState(
    val members: List<AppUser> = emptyList(),
    val currentUserId: String? = null,
    val inviteLink: String? = null,
    val isLoading: Boolean = true,
    /** Mitglieder, für die gerade eine Änderung gespeichert wird. */
    val busyMemberIds: Set<String> = emptySet(),
    override val errorMessage: String? = null,
    val canManage: Boolean = false,
    /** Die aus der [UserPermissionPolicy] abgeleiteten Rechte je Mitglieds-ID. */
    val memberPermissions: Map<String, MemberPermissions> = emptyMap(),
    val editor: MemberEditorState? = null,
    override val isSaving: Boolean = false,
) : ListDetailState {
    override val hasDetail: Boolean get() = editor != null

    /** Freigabe und Rollen ändern sich sofort; deshalb gilt für das gewählte Mitglied der Stand aus der Liste. */
    val selectedMember: AppUser?
        get() = editor?.member?.let { selected -> members.firstOrNull { it.uid == selected.uid } ?: selected }

    fun permissionsOf(member: AppUser): MemberPermissions = memberPermissions[member.uid] ?: MemberPermissions()
}

/** Verwaltung der Mitglieder der Versammlung des angemeldeten Versammlungsadmins. */
@Inject
class UserManagementViewModel(
    private val observeMembers: ObserveMembersUseCase,
    private val setMemberApproval: SetMemberApprovalUseCase,
    private val setMemberRole: SetMemberRoleUseCase,
    private val createMemberWithoutAccount: CreateMemberWithoutAccountUseCase,
    private val updateMemberProfile: UpdateMemberProfileUseCase,
    private val removeMemberUseCase: RemoveMemberUseCase,
    private val permissionPolicy: UserPermissionPolicy,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(UserManagementUiState())
    val uiState = mutableUiState.asStateFlow()

    private var currentUser: AppUser? = null
    private var observeJob: Job? = null
    private var observedCongregation: CongregationRef? = null
    private var observing = false
    private var inviteLinkCongregation: CongregationRef? = null
    private var cachedInviteLink: String? = null

    fun onCurrentUserChanged(user: AppUser) {
        currentUser = user
        val canManage = permissionPolicy.canManageUsers(user)
        // Mitglieder werden nur beobachtet, solange der Nutzer sie verwalten darf.
        val congregation = user.congregation?.takeIf { canManage }
        updateState {
            it.copy(
                currentUserId = user.uid,
                inviteLink = inviteLinkFor(congregation),
                canManage = canManage,
                editor = it.editor?.takeIf { canManage },
            )
        }
        if (observing && congregation == observedCongregation) return

        observeJob?.cancel()
        observedCongregation = congregation
        observing = true
        if (congregation == null) {
            updateState { it.copy(members = emptyList(), isLoading = false) }
            return
        }
        updateState { it.copy(isLoading = true) }
        observeJob = viewModelScope.launch {
            observeMembers(congregation)
                .catch { exception ->
                    val message = exception.message ?: "Mitglieder konnten nicht geladen werden."
                    updateState { it.copy(isLoading = false, errorMessage = message) }
                }.collect { members ->
                    updateState { it.copy(members = members, isLoading = false) }
                }
        }
    }

    // Der Link ist bei jedem Verschlüsseln ein anderer; er wird gemerkt, damit die Anzeige nicht springt.
    private fun inviteLinkFor(congregation: CongregationRef?): String? {
        if (congregation != inviteLinkCongregation) {
            inviteLinkCongregation = congregation
            cachedInviteLink = congregation?.toInviteLink()
        }
        return cachedInviteLink
    }

    fun addMemberWithoutAccount() {
        val user = currentUser
        if (user == null || !permissionPolicy.canCreateMemberWithoutAccount(user)) {
            updateState { it.copy(errorMessage = MISSING_PERMISSION_MESSAGE) }
            return
        }
        updateState { it.copy(editor = MemberEditorState(), errorMessage = null) }
    }

    fun editProfile(member: AppUser) {
        if (!mutableUiState.value.permissionsOf(member).canEditProfile) {
            updateState { it.copy(errorMessage = MISSING_PERMISSION_MESSAGE) }
            return
        }
        updateState { it.copy(editor = MemberEditorState(member.profile, member), errorMessage = null) }
    }

    /** Hält die Angaben stimmig, z. B. verliert ein Kind die Taufe und ein Nicht-Bruder das Dienstamt. */
    fun updateEditor(profile: MemberProfile) {
        updateState { state ->
            state.copy(editor = state.editor?.copy(profile = profile.normalized().withinInputLimits()))
        }
    }

    fun cancelEditing() {
        if (mutableUiState.value.isSaving) return
        updateState { it.copy(editor = null, errorMessage = null) }
    }

    /**
     * Verwirft Änderungen, lässt aber das ausgewählte Mitglied offen; eine neue, ungespeicherte Person wird
     * geschlossen.
     */
    fun discardChanges() {
        if (mutableUiState.value.isSaving) return
        updateState { state ->
            val editor = state.editor?.takeUnless { it.isNew }?.let { it.copy(profile = it.original) }
            state.copy(editor = editor, errorMessage = null)
        }
    }

    fun saveProfile() {
        val state = mutableUiState.value
        val editor = state.editor ?: return
        val uid = editor.member?.uid
        if (state.isSaving || uid in state.busyMemberIds) return
        val user = currentUser ?: return

        updateState {
            it.copy(isSaving = true, busyMemberIds = it.busyMemberIds + setOfNotNull(uid), errorMessage = null)
        }
        viewModelScope.launch {
            val result = if (editor.member == null) {
                createMemberWithoutAccount(user, editor.profile)
            } else {
                updateMemberProfile(user, editor.member.copy(profile = editor.profile))
            }
            result.onSuccess { saved ->
                updateState { current ->
                    val members = if (current.members.any { it.uid == saved.uid }) {
                        current.members.map { if (it.uid == saved.uid) saved else it }
                    } else {
                        current.members + saved
                    }
                    current.copy(
                        members = members,
                        editor = MemberEditorState(saved.profile, saved),
                        isSaving = false,
                        busyMemberIds = current.busyMemberIds - saved.uid,
                    )
                }
            }.onFailure { exception ->
                updateState {
                    it.copy(
                        isSaving = false,
                        busyMemberIds = it.busyMemberIds - setOfNotNull(uid),
                        errorMessage = exception.message ?: "Angaben konnten nicht gespeichert werden.",
                    )
                }
            }
        }
    }

    fun setApproved(member: AppUser, approved: Boolean) = updateMemberWith(member) { user ->
        setMemberApproval(user, member, approved)
    }

    fun toggleRole(member: AppUser, role: CongregationRole) = updateMemberWith(member) { user ->
        setMemberRole(user, member, role, granted = role !in member.roles, members = mutableUiState.value.members)
    }

    fun removeMember(member: AppUser) {
        val user = startMemberChange(member) ?: return
        viewModelScope.launch {
            removeMemberUseCase(user, member)
                .onSuccess {
                    updateState { state ->
                        state.copy(
                            members = state.members.filterNot { it.uid == member.uid },
                            editor = state.editor?.takeUnless { it.member?.uid == member.uid },
                            busyMemberIds = state.busyMemberIds - member.uid,
                        )
                    }
                }.onFailure { exception -> fail(member.uid, exception, "Mitglied konnte nicht entfernt werden.") }
        }
    }

    fun dismissError() {
        updateState { it.copy(errorMessage = null) }
    }

    private fun updateMemberWith(member: AppUser, change: suspend (AppUser) -> Result<AppUser>) {
        val user = startMemberChange(member) ?: return
        viewModelScope.launch {
            change(user)
                .onSuccess { saved ->
                    updateState { state ->
                        state.copy(
                            members = state.members.map { if (it.uid == saved.uid) saved else it },
                            busyMemberIds = state.busyMemberIds - saved.uid,
                        )
                    }
                }.onFailure { exception -> fail(member.uid, exception, "Mitglied konnte nicht gespeichert werden.") }
        }
    }

    /** Markiert [member] als beschäftigt und liefert den angemeldeten Nutzer, sofern für [member] nichts anderes läuft. */
    private fun startMemberChange(member: AppUser): AppUser? {
        if (member.uid in mutableUiState.value.busyMemberIds) return null
        val user = currentUser ?: return null
        markBusy(member.uid)
        return user
    }

    // Die Rechte werden bei jeder Zustandsänderung neu abgeleitet, damit sie zu Nutzer und Mitgliederliste passen.
    private fun updateState(transform: (UserManagementUiState) -> UserManagementUiState) {
        mutableUiState.update { state ->
            val newState = transform(state)
            newState.copy(memberPermissions = permissionsFor(newState.members))
        }
    }

    private fun permissionsFor(members: List<AppUser>): Map<String, MemberPermissions> {
        val user = currentUser ?: return emptyMap()
        return members.associate { member ->
            member.uid to MemberPermissions(
                canEdit = permissionPolicy.canEditMember(user, member),
                canEditProfile = permissionPolicy.canEditProfile(user, member),
                toggleableRoles = CongregationRole.entries.filterTo(mutableSetOf()) { role ->
                    permissionPolicy.canToggleRole(user, member, role, members)
                },
                adminRoleLockReason = permissionPolicy.adminRoleLock(user, member, members)?.let(::lockReason),
            )
        }
    }

    private fun lockReason(lock: AdminRoleLock): String = when (lock) {
        AdminRoleLock.OWN_ROLE -> "Die eigene Admin-Rolle bleibt bestehen."
        AdminRoleLock.LAST_ADMIN -> "Eine Versammlung braucht mindestens einen Admin."
    }

    private fun markBusy(uid: String) {
        updateState { it.copy(busyMemberIds = it.busyMemberIds + uid, errorMessage = null) }
    }

    private fun fail(uid: String, exception: Throwable, fallback: String) {
        updateState {
            it.copy(busyMemberIds = it.busyMemberIds - uid, errorMessage = exception.message ?: fallback)
        }
    }

    private companion object {
        const val MISSING_PERMISSION_MESSAGE = "Dafür fehlt dir die Berechtigung."
    }
}
