package de.geosphere.congregationplaner.congregation.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
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
            midweekMeetingTime = "19:00",
            midweekMeetingWeekday = "Mittwoch",
            weekendMeetingTime = "10:00",
            weekendMeetingWeekday = "Sonntag",
        )
        val repository = FakeCongregationMetadataRepository(emptyList())

        assertEquals(expected, SaveCongregationUseCase(repository)(expected).getOrThrow())
        assertEquals(expected, repository.savedCongregation)
    }

    private class FakeCongregationMetadataRepository(private val congregations: List<CongregationMetadata>) :
        CongregationMetadataRepository {
        var savedCongregation: CongregationMetadata? = null
            private set

        override fun observeAll(): Flow<List<CongregationMetadata>> = flowOf(congregations)

        override suspend fun save(congregation: CongregationMetadata): Result<CongregationMetadata> {
            savedCongregation = congregation
            return Result.success(congregation)
        }
    }
}
