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
    private val circuit = listOf("EMEA", "europe", "central-europe", "609a")

    @Test
    fun `observe use case returns the congregations of the circuit`() = runBlocking {
        val expected = listOf(CongregationMetadata(id = "k1", name = "Berlin"))
        val repository = FakeCongregationMetadataRepository(expected)

        assertEquals(expected, ObserveCongregationsUseCase(repository)(circuit).first())
        assertEquals(circuit, repository.circuit)
    }

    @Test
    fun `save use case delegates congregation and circuit to repository`() = runBlocking {
        val expected = CongregationMetadata(
            id = "k1",
            name = "Berlin",
            midweekMeetingTime = LocalTime(19, 0),
            midweekMeetingWeekday = 3,
            weekendMeetingTime = LocalTime(10, 0),
            weekendMeetingWeekday = 7,
        )
        val repository = FakeCongregationMetadataRepository(emptyList())

        assertEquals(expected, SaveCongregationUseCase(repository)(circuit, expected).getOrThrow())
        assertEquals(expected, repository.savedCongregation)
        assertEquals(circuit, repository.circuit)
    }

    @Test
    fun `delete use case delegates congregation id and circuit to repository`() = runBlocking {
        val repository = FakeCongregationMetadataRepository(emptyList())

        assertEquals(Unit, DeleteCongregationUseCase(repository)(circuit, "k1").getOrThrow())
        assertEquals("k1", repository.deletedCongregationId)
        assertEquals(circuit, repository.circuit)
    }

    private class FakeCongregationMetadataRepository(private val congregations: List<CongregationMetadata>) :
        CongregationMetadataRepository {
        var savedCongregation: CongregationMetadata? = null
            private set

        var deletedCongregationId: String? = null
            private set

        var circuit: List<String>? = null
            private set

        override fun observeAll(circuitIds: List<String>): Flow<List<CongregationMetadata>> {
            circuit = circuitIds
            return flowOf(congregations)
        }

        override suspend fun save(
            circuitIds: List<String>,
            congregation: CongregationMetadata,
        ): Result<CongregationMetadata> {
            circuit = circuitIds
            savedCongregation = congregation
            return Result.success(congregation)
        }

        override suspend fun delete(circuitIds: List<String>, congregationId: String): Result<Unit> {
            circuit = circuitIds
            deletedCongregationId = congregationId
            return Result.success(Unit)
        }
    }
}
