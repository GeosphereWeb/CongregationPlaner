package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfileException
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject

/** Der Versammlungsadmin [user] gibt [member] frei oder nimmt die Freigabe zurück. */
@Inject
class SetMemberApprovalUseCase(private val repository: UserRepository, private val policy: UserPermissionPolicy) {
    suspend operator fun invoke(user: AppUser, member: AppUser, approved: Boolean): Result<AppUser> {
        if (member.uid.isBlank()) return Result.failure(IllegalArgumentException("Das Mitglied hat keine ID."))
        val congregation = user.congregation
        if (congregation == null || !policy.canEditMember(user, member)) {
            return Result.failure(MemberProfileException.NotAllowed())
        }
        if (approved && !member.hasAccount) return Result.failure(MemberProfileException.AccessWithoutAccount())
        return repository.updateMember(congregation, member.copy(approved = approved))
    }
}
