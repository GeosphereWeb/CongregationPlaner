package de.geosphere.congregationplaner.congregation.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.model.CongregationException
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.model.NewCongregationAdmin
import de.geosphere.congregationplaner.congregation.domain.permission.CongregationPermissionPolicy
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class CongregationUseCasesTest {
    private val circuit = listOf("EMEA", "europe", "central-europe", "609a")
    private val policy = CongregationPermissionPolicy()
    private val globalAdmin = AppUser(uid = "global-uid", isGlobalAdmin = true)

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

        assertEquals(expected, SaveCongregationUseCase(repository, policy)(globalAdmin, circuit, expected).getOrThrow())
        assertEquals(expected, repository.savedCongregation)
        assertEquals(circuit, repository.circuit)
    }

    @Test
    fun `delete use case delegates congregation id and circuit to repository`() = runBlocking {
        val repository = FakeCongregationMetadataRepository(emptyList())

        val congregation = CongregationMetadata(id = "k1", name = "Berlin")

        assertEquals(
            Unit,
            DeleteCongregationUseCase(repository, policy)(globalAdmin, circuit, congregation).getOrThrow(),
        )
        assertEquals("k1", repository.deletedCongregationId)
        assertEquals(circuit, repository.circuit)
    }

    @Test
    fun `only a global admin deletes a congregation`() = runBlocking {
        val repository = FakeCongregationMetadataRepository(emptyList())
        val member = AppUser(uid = "u1", approved = true)

        val failure = DeleteCongregationUseCase(repository, policy)(member, circuit, CongregationMetadata(id = "k1"))

        assertIs<CongregationException.NotAllowed>(failure.exceptionOrNull())
        assertNull(repository.deletedCongregationId)
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

        var nameOwner: String? = null

        var createdBy: String? = null
            private set

        var firstAdmin: NewCongregationAdmin? = null
            private set

        override suspend fun findNameOwner(circuitIds: List<String>, name: String): Result<String?> =
            Result.success(nameOwner)

        override suspend fun create(
            circuitIds: List<String>,
            congregation: CongregationMetadata,
            createdBy: String,
            firstAdmin: NewCongregationAdmin?,
        ): Result<CongregationMetadata> {
            circuit = circuitIds
            this.createdBy = createdBy
            this.firstAdmin = firstAdmin
            savedCongregation = congregation
            return Result.success(congregation.copy(id = "new-id"))
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
