package de.geosphere.congregationplaner.user.presentation.management

import de.geosphere.congregationplaner.presentation.components.ListDetailState
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.model.MemberProfile

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
    /** Die aus der Policy abgeleiteten Rechte je Mitglieds-ID. */
    val memberPermissions: Map<String, MemberPermissions> = emptyMap(),
    val editor: MemberEditorState? = null,
    override val isSaving: Boolean = false,
) : ListDetailState {
    override val hasDetail: Boolean get() = editor != null

    /** Freigabe und Rollen ändern sich sofort; deshalb gilt für das gewählte Mitglied der Stand aus der Liste. */
    val selectedMember: AppUser?
        get() = editor?.member?.let { selected -> members.firstOrNull { it.uid == selected.uid } ?: selected }

    fun permissionsOf(member: AppUser): MemberPermissions = memberPermissions[member.uid] ?: MemberPermissions()

    /** Übernimmt ein gespeichertes Mitglied in die Liste: ersetzt es oder hängt es an. */
    fun withSavedMember(saved: AppUser): UserManagementUiState = copy(
        members = if (members.any { it.uid == saved.uid }) {
            members.map { if (it.uid == saved.uid) saved else it }
        } else {
            members + saved
        },
        busyMemberIds = busyMemberIds - saved.uid,
    )
}

internal const val MISSING_PERMISSION_MESSAGE = "Dafür fehlt dir die Berechtigung."
