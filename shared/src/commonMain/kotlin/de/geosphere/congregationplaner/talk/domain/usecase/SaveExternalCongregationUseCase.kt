package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectoryException
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

@Inject
class SaveExternalCongregationUseCase(
    private val repository: ExternalSpeakerDirectoryRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(
        user: AppUser,
        externalCongregation: ExternalCongregation,
    ): Result<ExternalCongregation> {
        val congregation = user.plannerCongregation(policy)
            ?: return Result.failure(ExternalDirectoryException.NotAllowed())
        val cleaned = externalCongregation.copy(name = externalCongregation.name.trim())
        val valid = cleaned.name.isNotEmpty() &&
            cleaned.name.length <= MAX_NAME &&
            repository.observeCircuits(congregation).first().any { it.id == cleaned.circuitId }
        if (!valid) return Result.failure(ExternalDirectoryException.InvalidCongregation())
        return repository.saveCongregation(congregation, cleaned)
    }
}
