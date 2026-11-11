package de.geosphere.congregationplaner.user.presentation.management

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.permission.AdminRoleLock
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy

/** Die Rechte von [user] an jedem der [members], je Mitglieds-ID; ohne Nutzer keine. */
internal fun memberPermissions(
    policy: UserPermissionPolicy,
    user: AppUser?,
    members: List<AppUser>,
): Map<String, MemberPermissions> {
    if (user == null) return emptyMap()
    return members.associate { member ->
        member.uid to MemberPermissions(
            canEdit = policy.canEditMember(user, member),
            canEditProfile = policy.canEditProfile(user, member),
            toggleableRoles = CongregationRole.entries.filterTo(mutableSetOf()) { role ->
                policy.canToggleRole(user, member, role, members)
            },
            adminRoleLockReason = policy.adminRoleLock(user, member, members)?.let(::lockReason),
        )
    }
}

private fun lockReason(lock: AdminRoleLock): String = when (lock) {
    AdminRoleLock.OWN_ROLE -> "Die eigene Admin-Rolle bleibt bestehen."
    AdminRoleLock.LAST_ADMIN -> "Eine Versammlung braucht mindestens einen Admin."
}
