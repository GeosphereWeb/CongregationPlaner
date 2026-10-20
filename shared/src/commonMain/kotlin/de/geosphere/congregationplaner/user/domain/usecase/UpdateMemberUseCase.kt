package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject

@Inject
class UpdateMemberUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(congregation: CongregationRef, member: AppUser): Result<AppUser> {
        if (member.uid.isBlank()) return Result.failure(IllegalArgumentException("Das Mitglied hat keine ID."))
        if (member.role !in UserRole.assignable) {
            val message = "Die Rolle ${member.role.label} kann nicht vergeben werden."
            return Result.failure(IllegalArgumentException(message))
        }
        return repository.updateMember(congregation, member)
    }
}
