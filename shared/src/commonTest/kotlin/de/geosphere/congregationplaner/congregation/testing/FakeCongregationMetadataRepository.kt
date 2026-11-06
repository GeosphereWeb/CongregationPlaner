package de.geosphere.congregationplaner.congregation.testing

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.model.NewCongregationAdmin
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/** [CongregationMetadataRepository], das feste Versammlungen liefert oder mit [error] ausfällt; merkt sich den Kreis. */
internal class FakeCongregationMetadataRepository(
    private val congregations: List<CongregationMetadata> = emptyList(),
    private val error: Throwable? = null,
) : CongregationMetadataRepository {
    val requestedCircuits = mutableListOf<List<String>>()

    override fun observeAll(circuitIds: List<String>): Flow<List<CongregationMetadata>> {
        requestedCircuits += circuitIds
        return error?.let { flow { throw it } } ?: flowOf(congregations)
    }

    override suspend fun findNameOwner(circuitIds: List<String>, name: String): Result<String?> = Result.success(null)

    override suspend fun create(
        circuitIds: List<String>,
        congregation: CongregationMetadata,
        createdBy: String,
        firstAdmin: NewCongregationAdmin?,
    ): Result<CongregationMetadata> = Result.success(congregation.copy(id = "new-id"))

    override suspend fun save(
        circuitIds: List<String>,
        congregation: CongregationMetadata,
    ): Result<CongregationMetadata> = Result.success(congregation)

    override suspend fun delete(circuitIds: List<String>, congregationId: String): Result<Unit> = Result.success(Unit)
}
