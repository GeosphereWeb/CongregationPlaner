package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.model.MemberProfileException
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject

/**
 * Der Versammlungsadmin [user] gibt [member] die Rolle [role] oder nimmt sie ihm. [members] sind alle Mitglieder der
 * Versammlung; sie entscheiden, ob die Admin-Rolle bestehen bleiben muss.
 */
@Inject
class SetMemberRoleUseCase(private val repository: UserRepository, private val policy: UserPermissionPolicy) {
    suspend operator fun invoke(
        user: AppUser,
        member: AppUser,
        role: CongregationRole,
        granted: Boolean,
        members: List<AppUser>,
    ): Result<AppUser> {
        val congregation = user.congregation?.takeIf { policy.canToggleRole(user, member, role, members) }
        val roles = if (granted) member.roles + role else member.roles - role
        return when {
            member.uid.isBlank() -> Result.failure(IllegalArgumentException("Das Mitglied hat keine ID."))
            congregation == null -> Result.failure(MemberProfileException.NotAllowed())
            granted && !member.hasAccount -> Result.failure(MemberProfileException.AccessWithoutAccount())
            else -> repository.updateMember(congregation, member.copy(roles = roles))
        }
    }
}
