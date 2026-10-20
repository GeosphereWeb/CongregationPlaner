package de.geosphere.congregationplaner.circuit.domain.usecase

import de.geosphere.congregationplaner.circuit.domain.repository.CircuitRepository
import dev.zacsweers.metro.Inject

@Inject
class DeleteCircuitUseCase(private val repository: CircuitRepository) {
    suspend operator fun invoke(circuitId: String): Result<Unit> = repository.delete(circuitId)
}
