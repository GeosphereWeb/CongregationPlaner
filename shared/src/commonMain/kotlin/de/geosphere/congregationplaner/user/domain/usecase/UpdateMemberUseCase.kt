package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfileException
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject

@Inject
class UpdateMemberUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(congregation: CongregationRef, member: AppUser): Result<AppUser> {
        if (member.uid.isBlank()) return Result.failure(IllegalArgumentException("Das Mitglied hat keine ID."))
        val grantsAccess = member.approved || member.roles.isNotEmpty()
        if (!member.hasAccount && grantsAccess) {
            return Result.failure(MemberProfileException.AccessWithoutAccount())
        }
        return repository.updateMember(congregation, member)
    }
}
