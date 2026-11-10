package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfileException
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject

/** Der Versammlungsadmin [user] entfernt [member] aus der Versammlung. */
@Inject
class RemoveMemberUseCase(private val repository: UserRepository, private val policy: UserPermissionPolicy) {
    suspend operator fun invoke(user: AppUser, member: AppUser): Result<Unit> {
        if (member.uid.isBlank()) return Result.failure(IllegalArgumentException("Das Mitglied hat keine ID."))
        val congregation = user.congregation
        if (congregation == null || !policy.canEditMember(user, member)) {
            return Result.failure(MemberProfileException.NotAllowed())
        }
        return repository.removeMember(congregation, member.uid)
    }
}
