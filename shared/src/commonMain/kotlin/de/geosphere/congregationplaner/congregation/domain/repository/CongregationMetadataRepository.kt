package de.geosphere.congregationplaner.congregation.domain.repository

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import kotlinx.coroutines.flow.Flow

/**
 * Die Versammlungen eines Kreises. [circuitIds] sind die IDs von der Region bis zum Kreis (siehe
 * `CongregationRef.circuitIds` und `OrgLevel.collectionPath`).
 */
interface CongregationMetadataRepository {
    fun observeAll(circuitIds: List<String>): Flow<List<CongregationMetadata>>

    suspend fun save(circuitIds: List<String>, congregation: CongregationMetadata): Result<CongregationMetadata>

    suspend fun delete(circuitIds: List<String>, congregationId: String): Result<Unit>
}
