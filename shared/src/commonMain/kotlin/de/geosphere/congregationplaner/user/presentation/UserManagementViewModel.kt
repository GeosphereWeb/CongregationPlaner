package de.geosphere.congregationplaner.user.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.model.UserPermission
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.usecase.CreateMemberWithoutAccountUseCase
import de.geosphere.congregationplaner.user.domain.usecase.ObserveMembersUseCase
import de.geosphere.congregationplaner.user.domain.usecase.RemoveMemberUseCase
import de.geosphere.congregationplaner.user.domain.usecase.UpdateMemberProfileUseCase
import de.geosphere.congregationplaner.user.domain.usecase.UpdateMemberUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Die gerade bearbeiteten persönlichen Angaben; [member] ist `null`, solange eine neue Person ohne Konto entsteht. */
data class MemberEditorState(val profile: MemberProfile = MemberProfile(), val member: AppUser? = null) {
    val isNew: Boolean get() = member == null
}

data class UserManagementUiState(
    val members: List<AppUser> = emptyList(),
    val currentUserId: String? = null,
    val inviteLink: String? = null,
    val isLoading: Boolean = true,
    /** Mitglieder, für die gerade eine Änderung gespeichert wird. */
    val busyMemberIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val canManage: Boolean = false,
    val editor: MemberEditorState? = null,
    val isSaving: Boolean = false,
)

/** Verwaltung der Mitglieder der Versammlung des angemeldeten Versammlungsadmins. */
@Inject
class UserManagementViewModel(
    private val observeMembers: ObserveMembersUseCase,
    private val updateMember: UpdateMemberUseCase,
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
        mutableUiState.update {
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
            mutableUiState.update { it.copy(members = emptyList(), isLoading = false) }
            return
        }
        mutableUiState.update { it.copy(isLoading = true) }
        observeJob = viewModelScope.launch {
            observeMembers(congregation)
                .catch { exception ->
                    val message = exception.message ?: "Mitglieder konnten nicht geladen werden."
                    mutableUiState.update { it.copy(isLoading = false, errorMessage = message) }
                }.collect { members ->
                    mutableUiState.update { it.copy(members = members, isLoading = false) }
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

    fun canEdit(member: AppUser): Boolean = currentUser?.let { permissionPolicy.canEditMember(it, member) } ?: false

    fun canEditProfile(member: AppUser): Boolean =
        currentUser?.let { permissionPolicy.canEditProfile(it, member) } ?: false

    fun addMemberWithoutAccount() {
        val user = currentUser
        if (user == null || !permissionPolicy.canCreateMemberWithoutAccount(user)) {
            mutableUiState.update { it.copy(errorMessage = MISSING_PERMISSION_MESSAGE) }
            return
        }
        mutableUiState.update { it.copy(editor = MemberEditorState(), errorMessage = null) }
    }

    fun editProfile(member: AppUser) {
        if (!canEditProfile(member)) {
            mutableUiState.update { it.copy(errorMessage = MISSING_PERMISSION_MESSAGE) }
            return
        }
        mutableUiState.update { it.copy(editor = MemberEditorState(member.profile, member), errorMessage = null) }
    }

    /** Hält die Angaben stimmig, z. B. verliert ein Kind die Taufe und ein Nicht-Bruder das Dienstamt. */
    fun updateEditor(profile: MemberProfile) {
        mutableUiState.update { state ->
            state.copy(editor = state.editor?.copy(profile = profile.normalized()))
        }
    }

    fun cancelEditing() {
        if (mutableUiState.value.isSaving) return
        mutableUiState.update { it.copy(editor = null, errorMessage = null) }
    }

    fun saveProfile() {
        val state = mutableUiState.value
        val editor = state.editor ?: return
        val uid = editor.member?.uid
        if (state.isSaving || uid in state.busyMemberIds) return
        val congregation = profileCongregation(editor) ?: return

        mutableUiState.update {
            it.copy(isSaving = true, busyMemberIds = it.busyMemberIds + setOfNotNull(uid), errorMessage = null)
        }
        viewModelScope.launch {
            val result = if (editor.member == null) {
                createMemberWithoutAccount(congregation, editor.profile)
            } else {
                updateMemberProfile(congregation, editor.member.copy(profile = editor.profile))
            }
            result.onSuccess { saved ->
                mutableUiState.update { current ->
                    val members = if (current.members.any { it.uid == saved.uid }) {
                        current.members.map { if (it.uid == saved.uid) saved else it }
                    } else {
                        current.members + saved
                    }
                    current.copy(
                        members = members,
                        editor = null,
                        isSaving = false,
                        busyMemberIds = current.busyMemberIds - saved.uid,
                    )
                }
            }.onFailure { exception ->
                mutableUiState.update {
                    it.copy(
                        isSaving = false,
                        busyMemberIds = it.busyMemberIds - setOfNotNull(uid),
                        errorMessage = exception.message ?: "Angaben konnten nicht gespeichert werden.",
                    )
                }
            }
        }
    }

    fun setApproved(member: AppUser, approved: Boolean) = save(member.copy(approved = approved))

    fun setRole(member: AppUser, role: UserRole) = save(member.copy(role = role))

    fun togglePermission(member: AppUser, permission: UserPermission) {
        val permissions = if (permission in member.permissions) {
            member.permissions - permission
        } else {
            member.permissions + permission
        }
        save(member.copy(permissions = permissions))
    }

    fun removeMember(member: AppUser) {
        val congregation = checkedCongregation(member) ?: return
        markBusy(member.uid)
        viewModelScope.launch {
            removeMemberUseCase(congregation, member.uid)
                .onSuccess {
                    mutableUiState.update { state ->
                        state.copy(
                            members = state.members.filterNot { it.uid == member.uid },
                            busyMemberIds = state.busyMemberIds - member.uid,
                        )
                    }
                }.onFailure { exception -> fail(member.uid, exception, "Mitglied konnte nicht entfernt werden.") }
        }
    }

    fun dismissError() {
        mutableUiState.update { it.copy(errorMessage = null) }
    }

    private fun save(member: AppUser) {
        val congregation = checkedCongregation(member) ?: return
        markBusy(member.uid)
        viewModelScope.launch {
            updateMember(congregation, member)
                .onSuccess { saved ->
                    mutableUiState.update { state ->
                        state.copy(
                            members = state.members.map { if (it.uid == saved.uid) saved else it },
                            busyMemberIds = state.busyMemberIds - saved.uid,
                        )
                    }
                }.onFailure { exception -> fail(member.uid, exception, "Mitglied konnte nicht gespeichert werden.") }
        }
    }

    /** Liefert die Versammlung, sofern der angemeldete Nutzer [member] ändern darf und für ihn nichts anderes läuft. */
    private fun checkedCongregation(member: AppUser): CongregationRef? {
        if (member.uid in mutableUiState.value.busyMemberIds) return null
        val congregation = currentUser?.takeIf { permissionPolicy.canEditMember(it, member) }?.congregation
        if (congregation == null) mutableUiState.update { it.copy(errorMessage = MISSING_PERMISSION_MESSAGE) }
        return congregation
    }

    /** Liefert die Versammlung, sofern der angemeldete Nutzer die Angaben aus [editor] speichern darf. */
    private fun profileCongregation(editor: MemberEditorState): CongregationRef? {
        val user = currentUser
        val allowed = when {
            user == null -> false
            editor.member == null -> permissionPolicy.canCreateMemberWithoutAccount(user)
            else -> permissionPolicy.canEditProfile(user, editor.member)
        }
        val congregation = user?.congregation?.takeIf { allowed }
        if (congregation == null) mutableUiState.update { it.copy(errorMessage = MISSING_PERMISSION_MESSAGE) }
        return congregation
    }

    private fun markBusy(uid: String) {
        mutableUiState.update { it.copy(busyMemberIds = it.busyMemberIds + uid, errorMessage = null) }
    }

    private fun fail(uid: String, exception: Throwable, fallback: String) {
        mutableUiState.update {
            it.copy(busyMemberIds = it.busyMemberIds - uid, errorMessage = exception.message ?: fallback)
        }
    }

    private companion object {
        const val MISSING_PERMISSION_MESSAGE = "Dafür fehlt dir die Berechtigung."
    }
}
