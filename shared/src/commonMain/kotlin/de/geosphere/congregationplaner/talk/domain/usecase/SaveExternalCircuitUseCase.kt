package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectoryException
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

@Inject
class SaveExternalCircuitUseCase(
    private val repository: ExternalSpeakerDirectoryRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, circuit: ExternalCircuit): Result<ExternalCircuit> {
        val congregation = user.plannerCongregation(policy)
            ?: return Result.failure(ExternalDirectoryException.NotAllowed())
        val cleaned = circuit.copy(name = circuit.name.trim())
        if (cleaned.name.isEmpty() || cleaned.name.length > MAX_NAME) {
            return Result.failure(ExternalDirectoryException.InvalidCircuit())
        }
        return repository.saveCircuit(congregation, cleaned)
    }
}
