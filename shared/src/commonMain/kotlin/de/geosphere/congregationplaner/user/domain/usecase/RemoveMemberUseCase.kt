package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject

@Inject
class RemoveMemberUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(congregation: CongregationRef, uid: String): Result<Unit> {
        if (uid.isBlank()) return Result.failure(IllegalArgumentException("Das Mitglied hat keine ID."))
        return repository.removeMember(congregation, uid)
    }
}
