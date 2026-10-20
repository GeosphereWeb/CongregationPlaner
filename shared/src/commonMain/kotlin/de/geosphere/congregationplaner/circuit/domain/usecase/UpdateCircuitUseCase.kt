package de.geosphere.congregationplaner.circuit.domain.usecase

import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.circuit.domain.repository.CircuitRepository
import dev.zacsweers.metro.Inject

@Inject
class UpdateCircuitUseCase(private val repository: CircuitRepository) {
    suspend operator fun invoke(circuit: Circuit): Result<Circuit> = circuit.validated().fold(
        onSuccess = { repository.update(it) },
        onFailure = { Result.failure(it) },
    )
}
