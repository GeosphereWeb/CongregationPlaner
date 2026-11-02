package de.geosphere.congregationplaner.congregation.testing

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/** [CongregationMetadataRepository], das feste Versammlungen liefert oder mit [error] ausfällt. */
internal class FakeCongregationMetadataRepository(
    private val congregations: List<CongregationMetadata> = emptyList(),
    private val error: Throwable? = null,
) : CongregationMetadataRepository {
    override fun observeAll(): Flow<List<CongregationMetadata>> =
        error?.let { flow { throw it } } ?: flowOf(congregations)

    override suspend fun save(congregation: CongregationMetadata): Result<CongregationMetadata> =
        Result.success(congregation)

    override suspend fun delete(congregationId: String): Result<Unit> = Result.success(Unit)
}
