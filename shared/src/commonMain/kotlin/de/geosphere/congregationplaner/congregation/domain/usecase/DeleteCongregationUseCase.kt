package de.geosphere.congregationplaner.congregation.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.model.CongregationException
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.permission.CongregationPermissionPolicy
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

@Inject
class DeleteCongregationUseCase(
    private val repository: CongregationMetadataRepository,
    private val policy: CongregationPermissionPolicy,
) {
    suspend operator fun invoke(
        user: AppUser,
        circuitIds: List<String>,
        congregation: CongregationMetadata,
    ): Result<Unit> {
        if (!policy.canDelete(user, congregation)) return Result.failure(CongregationException.NotAllowed())
        return repository.delete(circuitIds, congregation.id)
    }
}
