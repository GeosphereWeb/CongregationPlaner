package de.geosphere.congregationplaner.congregation.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import dev.zacsweers.metro.Inject

@Inject
class SaveCongregationUseCase(private val repository: CongregationMetadataRepository) {
    suspend operator fun invoke(
        circuitIds: List<String>,
        congregation: CongregationMetadata,
    ): Result<CongregationMetadata> = repository.save(circuitIds, congregation)
}
