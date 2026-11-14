package de.geosphere.congregationplaner.user.presentation.management

import de.geosphere.congregationplaner.presentation.components.ListDetailState
import de.geosphere.congregationplaner.talk.presentation.directory.toText
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.model.familyHeadCandidates
import de.geosphere.congregationplaner.user.domain.model.familyOf

/**
 * Die gerade bearbeiteten persönlichen Angaben; [member] ist `null`, solange eine neue Person ohne Konto entsteht.
 * [talksText] ist die Eingabe der Vortragsnummern; sie wird erst beim Speichern in Zahlen umgewandelt.
 */
data class MemberEditorState(
    val profile: MemberProfile = MemberProfile(),
    val member: AppUser? = null,
    val original: MemberProfile = profile,
    val talksText: String = profile.talkNumbers.toText(),
) {
    val isNew: Boolean get() = member == null
    val isDirty: Boolean get() = profile != original || talksText != original.talkNumbers.toText()
}

/**
 * Die Familie im Editor (ADR-0032): das gewählte Oberhaupt, die wählbaren Oberhäupter und, wenn das Mitglied selbst
 * Oberhaupt ist, seine Familie. Ein Oberhaupt kann keinem anderen zugeordnet werden, [candidates] ist dann leer.
 */
data class EditorFamily(
    val head: AppUser? = null,
    val candidates: List<AppUser> = emptyList(),
    val relatives: List<AppUser> = emptyList(),
) {
    val isHead: Boolean get() = relatives.isNotEmpty()
    val relativesText: String get() = relatives.joinToString(", ") { it.label }
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
    /** Darf Redner übernehmen und Konten verknüpfen (Versammlungsadmin mit Vortragsplaner-Rolle). */
    val canTransfer: Boolean = false,
    val adoptDialog: AdoptSpeakersDialogState? = null,
    val linkDialog: LinkAccountDialogState? = null,
    val infoMessage: String? = null,
    /** Personen ohne Konto, mit denen sich ein Konto verknüpfen lässt (vom ViewModel über den UseCase abgeleitet). */
    val linkCandidates: List<AppUser> = emptyList(),
) : ListDetailState {
    override val hasDetail: Boolean get() = editor != null

    /** Freigabe und Rollen ändern sich sofort; deshalb gilt für das gewählte Mitglied der Stand aus der Liste. */
    val selectedMember: AppUser?
        get() = editor?.member?.let { selected -> members.firstOrNull { it.uid == selected.uid } ?: selected }

    val editorFamily: EditorFamily
        get() {
            val uid = editor?.member?.uid
            return EditorFamily(
                head = editor?.profile?.familyHeadId?.let { head -> members.firstOrNull { it.uid == head } },
                candidates = familyHeadCandidates(members, uid),
                relatives = uid?.let { familyOf(members, it) }.orEmpty(),
            )
        }

    fun permissionsOf(member: AppUser): MemberPermissions = memberPermissions[member.uid] ?: MemberPermissions()

    /** „Mit Person ohne Konto verknüpfen“ gibt es nur für Konten und nur, wenn es eine solche Person gibt. */
    fun canLink(member: AppUser): Boolean = canTransfer && member.hasAccount && linkCandidates.isNotEmpty()

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
