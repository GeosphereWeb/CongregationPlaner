package de.geosphere.congregationplaner.circuit.domain.usecase

import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.circuit.domain.model.CircuitException
import de.geosphere.congregationplaner.circuit.domain.repository.CircuitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CircuitUseCasesTest {
    private val repository = FakeCircuitRepository(
        listOf(Circuit(id = "610b", name = "Süd"), Circuit(id = "609a", name = "Nord")),
    )

    @Test
    fun `observe use case returns circuits sorted by id`() = runBlocking {
        assertEquals(listOf("609a", "610b"), ObserveCircuitsUseCase(repository)().first().map { it.id })
    }

    @Test
    fun `create use case normalizes input before delegating`() = runBlocking {
        val created = CreateCircuitUseCase(repository)(
            Circuit(id = "  609A ", name = "  Berlin-Nord ", overseerName = " Max "),
        ).getOrThrow()

        val expected = Circuit(id = "609a", name = "Berlin-Nord", overseerName = "Max")
        assertEquals(expected, created)
        assertEquals(expected, repository.created)
    }

    @Test
    fun `create use case rejects invalid ids and missing names`() = runBlocking {
        val useCase = CreateCircuitUseCase(repository)

        listOf("", "609/a", "-609", "609-", "a".repeat(33), "kreis 1").forEach { id ->
            assertIs<CircuitException.InvalidId>(useCase(Circuit(id = id, name = "Berlin")).exceptionOrNull(), id)
        }
        assertIs<CircuitException.MissingName>(useCase(Circuit(id = "609a", name = "  ")).exceptionOrNull())
        assertNull(repository.created)
    }

    @Test
    fun `update use case trims fields and keeps id`() = runBlocking {
        val updated = UpdateCircuitUseCase(repository)(Circuit(id = "609a", name = " Nord ")).getOrThrow()

        assertEquals(Circuit(id = "609a", name = "Nord"), updated)
        assertEquals(updated, repository.updated)
    }

    @Test
    fun `update use case rejects missing name`() = runBlocking {
        val result = UpdateCircuitUseCase(repository)(Circuit(id = "609a", name = ""))

        assertIs<CircuitException.MissingName>(result.exceptionOrNull())
        assertNull(repository.updated)
    }

    @Test
    fun `delete use case delegates circuit id to repository`() = runBlocking {
        assertTrue(DeleteCircuitUseCase(repository)("609a").isSuccess)
        assertEquals("609a", repository.deletedId)
    }

    @Test
    fun `circuit id validation accepts typical circuit numbers`() {
        listOf("609a", "609", "ge-609-a", "a").forEach { assertTrue(Circuit.isValidId(it), it) }
        assertFalse(Circuit.isValidId("609A"))
        assertEquals("609a", Circuit.normalizeId(" 609A "))
    }

    private class FakeCircuitRepository(private val circuits: List<Circuit>) : CircuitRepository {
        var created: Circuit? = null
            private set
        var updated: Circuit? = null
            private set
        var deletedId: String? = null
            private set

        override fun observeAll(): Flow<List<Circuit>> = flowOf(circuits)

        override suspend fun create(circuit: Circuit): Result<Circuit> {
            created = circuit
            return Result.success(circuit)
        }

        override suspend fun update(circuit: Circuit): Result<Circuit> {
            updated = circuit
            return Result.success(circuit)
        }

        override suspend fun delete(circuitId: String): Result<Unit> {
            deletedId = circuitId
            return Result.success(Unit)
        }
    }
}
