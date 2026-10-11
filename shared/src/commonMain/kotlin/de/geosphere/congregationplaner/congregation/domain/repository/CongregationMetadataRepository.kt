package de.geosphere.congregationplaner.congregation.domain.repository

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import kotlinx.coroutines.flow.Flow

interface CongregationMetadataRepository {
    fun observeAll(): Flow<List<CongregationMetadata>>

    suspend fun save(congregation: CongregationMetadata): Result<CongregationMetadata>
}
