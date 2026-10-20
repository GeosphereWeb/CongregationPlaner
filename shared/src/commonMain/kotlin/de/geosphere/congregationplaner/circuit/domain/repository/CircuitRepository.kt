package de.geosphere.congregationplaner.circuit.domain.repository

import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.circuit.domain.model.CircuitException
import kotlinx.coroutines.flow.Flow

interface CircuitRepository {
    fun observeAll(): Flow<List<Circuit>>

    /** Legt den Kreis unter seiner [Circuit.id] an; ist sie vergeben, schlägt das mit [CircuitException.AlreadyExists] fehl. */
    suspend fun create(circuit: Circuit): Result<Circuit>

    suspend fun update(circuit: Circuit): Result<Circuit>

    /** Löscht den Kreis; solange ihm Versammlungen zugeordnet sind, schlägt das mit [CircuitException.HasCongregations] fehl. */
    suspend fun delete(circuitId: String): Result<Unit>
}
