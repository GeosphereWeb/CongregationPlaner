package de.geosphere.congregationplaner.congregation.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

/** Die Versammlungen des Kreises [circuitIds] (IDs von der Region bis zum Kreis). */
@Inject
class ObserveCongregationsUseCase(private val repository: CongregationMetadataRepository) {
    operator fun invoke(circuitIds: List<String>): Flow<List<CongregationMetadata>> = repository.observeAll(circuitIds)
}
