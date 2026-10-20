package de.geosphere.congregationplaner.congregation.data

import de.geosphere.congregationplaner.circuit.data.CIRCUIT_COLLECTION_PATH
import de.geosphere.congregationplaner.circuit.data.CONGREGATION_SUBCOLLECTION
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

expect fun createCongregationMetadataRepository(): CongregationMetadataRepository

internal const val CONGREGATION_COLLECTION_PATH = "$CIRCUIT_COLLECTION_PATH/609a/$CONGREGATION_SUBCOLLECTION"

fun unavailableCongregationMetadataRepository(platform: String): CongregationMetadataRepository =
    object : CongregationMetadataRepository {
        override fun observeAll(): Flow<List<CongregationMetadata>> = flow {
            throw UnsupportedOperationException("Firestore is not configured for $platform.")
        }

        override suspend fun save(congregation: CongregationMetadata): Result<CongregationMetadata> =
            Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

        override suspend fun delete(congregationId: String): Result<Unit> =
            Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))
    }
