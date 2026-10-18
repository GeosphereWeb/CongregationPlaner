package de.geosphere.congregationplaner.congregation.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

@Inject
class ObserveCongregationsUseCase(private val repository: CongregationMetadataRepository) {
    operator fun invoke(): Flow<List<CongregationMetadata>> = repository.observeAll()
}
