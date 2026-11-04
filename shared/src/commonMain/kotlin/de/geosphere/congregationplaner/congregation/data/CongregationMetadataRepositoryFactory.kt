package de.geosphere.congregationplaner.congregation.data

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

expect fun createCongregationMetadataRepository(): CongregationMetadataRepository

/** Die Versammlungs-Collection unter dem Kreis [circuitIds]. */
internal fun congregationCollectionPath(circuitIds: List<String>): String =
    OrgLevel.CONGREGATION.collectionPath(circuitIds)

fun unavailableCongregationMetadataRepository(platform: String): CongregationMetadataRepository =
    object : CongregationMetadataRepository {
        override fun observeAll(circuitIds: List<String>): Flow<List<CongregationMetadata>> = flow {
            throw UnsupportedOperationException("Firestore is not configured for $platform.")
        }

        override suspend fun save(
            circuitIds: List<String>,
            congregation: CongregationMetadata,
        ): Result<CongregationMetadata> =
            Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

        override suspend fun delete(circuitIds: List<String>, congregationId: String): Result<Unit> =
            Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))
    }
