package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject

/** Erfasst eine Person ohne eigenes Login (z. B. ein Kind) als Mitglied der Versammlung. */
@Inject
class CreateMemberWithoutAccountUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(congregation: CongregationRef, profile: MemberProfile): Result<AppUser> =
        profile.validated(requireName = true).fold(
            onSuccess = { repository.createMemberWithoutAccount(congregation, it) },
            onFailure = { Result.failure(it) },
        )
}
