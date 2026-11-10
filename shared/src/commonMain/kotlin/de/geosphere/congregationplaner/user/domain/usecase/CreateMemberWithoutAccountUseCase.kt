package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.model.MemberProfileException
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject

/** Der Versammlungsadmin [user] erfasst eine Person ohne eigenes Login (z. B. ein Kind) als Mitglied. */
@Inject
class CreateMemberWithoutAccountUseCase(
    private val repository: UserRepository,
    private val policy: UserPermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, profile: MemberProfile): Result<AppUser> {
        val congregation = user.congregation
        if (congregation == null || !policy.canCreateMemberWithoutAccount(user)) {
            return Result.failure(MemberProfileException.NotAllowed())
        }
        return profile.validated(requireName = true).fold(
            onSuccess = { repository.createMemberWithoutAccount(congregation, it) },
            onFailure = { Result.failure(it) },
        )
    }
}
