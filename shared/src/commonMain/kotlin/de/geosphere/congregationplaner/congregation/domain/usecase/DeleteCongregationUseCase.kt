package de.geosphere.congregationplaner.congregation.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import dev.zacsweers.metro.Inject

@Inject
class DeleteCongregationUseCase(private val repository: CongregationMetadataRepository) {
    suspend operator fun invoke(congregationId: String): Result<Unit> = repository.delete(congregationId)
}
