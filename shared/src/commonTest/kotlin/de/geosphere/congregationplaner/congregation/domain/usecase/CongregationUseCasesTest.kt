package de.geosphere.congregationplaner.congregation.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class CongregationUseCasesTest {
    @Test
    fun `observe use case returns repository congregations`() = runBlocking {
        val expected = listOf(CongregationMetadata(id = "609a", name = "Berlin"))
        val useCase = ObserveCongregationsUseCase(FakeCongregationMetadataRepository(expected))

        assertEquals(expected, useCase().first())
    }

    @Test
    fun `save use case delegates congregation to repository`() = runBlocking {
        val expected = CongregationMetadata(
            id = "609a",
            name = "Berlin",
            midweekMeetingTime = LocalTime(19, 0),
            midweekMeetingWeekday = 3,
            weekendMeetingTime = LocalTime(10, 0),
            weekendMeetingWeekday = 7,
        )
        val repository = FakeCongregationMetadataRepository(emptyList())

        assertEquals(expected, SaveCongregationUseCase(repository)(expected).getOrThrow())
        assertEquals(expected, repository.savedCongregation)
    }

    @Test
    fun `delete use case delegates congregation id to repository`() = runBlocking {
        val repository = FakeCongregationMetadataRepository(emptyList())

        assertEquals(Unit, DeleteCongregationUseCase(repository)("609a").getOrThrow())
        assertEquals("609a", repository.deletedCongregationId)
    }

    private class FakeCongregationMetadataRepository(private val congregations: List<CongregationMetadata>) :
        CongregationMetadataRepository {
        var savedCongregation: CongregationMetadata? = null
            private set

        var deletedCongregationId: String? = null
            private set

        override fun observeAll(): Flow<List<CongregationMetadata>> = flowOf(congregations)

        override suspend fun save(congregation: CongregationMetadata): Result<CongregationMetadata> {
            savedCongregation = congregation
            return Result.success(congregation)
        }

        override suspend fun delete(congregationId: String): Result<Unit> {
            deletedCongregationId = congregationId
            return Result.success(Unit)
        }
    }
}
