package de.geosphere.congregationplaner.circuit.domain.usecase

import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.circuit.domain.model.CircuitException
import de.geosphere.congregationplaner.circuit.domain.repository.CircuitRepository
import dev.zacsweers.metro.Inject

@Inject
class CreateCircuitUseCase(private val repository: CircuitRepository) {
    suspend operator fun invoke(circuit: Circuit): Result<Circuit> =
        circuit.copy(id = Circuit.normalizeId(circuit.id)).validated().fold(
            onSuccess = { repository.create(it) },
            onFailure = { Result.failure(it) },
        )
}

/** Trimmt die Eingaben und prüft Kreisnummer und Namen. */
internal fun Circuit.validated(): Result<Circuit> {
    val normalized = copy(name = name.trim(), overseerName = overseerName.trim())
    return when {
        !Circuit.isValidId(normalized.id) -> Result.failure(CircuitException.InvalidId())
        normalized.name.isEmpty() -> Result.failure(CircuitException.MissingName())
        else -> Result.success(normalized)
    }
}
