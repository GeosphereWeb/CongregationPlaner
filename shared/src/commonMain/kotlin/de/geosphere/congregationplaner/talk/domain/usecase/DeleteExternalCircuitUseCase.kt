package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectoryException
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

/** Entfernt einen Kreis; er muss leer sein, damit keine Versammlungen ohne Kreis zurückbleiben. */
@Inject
class DeleteExternalCircuitUseCase(
    private val repository: ExternalSpeakerDirectoryRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, id: String): Result<Unit> {
        val congregation = user.plannerCongregation(policy)
            ?: return Result.failure(ExternalDirectoryException.NotAllowed())
        if (id.isBlank()) return Result.failure(ExternalDirectoryException.InvalidCircuit())
        if (repository.observeCongregations(congregation).first().any { it.circuitId == id }) {
            return Result.failure(ExternalDirectoryException.CircuitNotEmpty())
        }
        return repository.deleteCircuit(congregation, id)
    }
}
