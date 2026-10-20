package de.geosphere.congregationplaner.circuit.domain.usecase

import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.circuit.domain.repository.CircuitRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Inject
class ObserveCircuitsUseCase(private val repository: CircuitRepository) {
    operator fun invoke(): Flow<List<Circuit>> = repository.observeAll().map { circuits -> circuits.sortedBy { it.id } }
}
