package de.geosphere.congregationplaner.congregation.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.model.CongregationException
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.permission.CongregationPermissionPolicy
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/**
 * Der globale Admin [user] legt eine Versammlung im Kreis [circuitIds] an, ohne ihr Mitglied zu werden (ADR-0013).
 * Der Name muss im Kreis noch frei sein.
 */
@Inject
class CreateCongregationUseCase(
    private val repository: CongregationMetadataRepository,
    private val policy: CongregationPermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, circuitIds: List<String>, name: String): Result<CongregationMetadata> {
        if (!policy.canCreate(user)) return Result.failure(CongregationException.NotAllowed())
        return repository.createWithFreeName(circuitIds, name, createdBy = user.uid, firstAdmin = null)
    }
}
