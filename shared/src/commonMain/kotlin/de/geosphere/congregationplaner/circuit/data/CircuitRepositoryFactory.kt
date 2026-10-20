package de.geosphere.congregationplaner.circuit.data

import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.circuit.domain.repository.CircuitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

expect fun createCircuitRepository(): CircuitRepository

internal const val CIRCUIT_COLLECTION_PATH = "region/EMEA/subregion/europe/branch/central-europe/circuit"

/** Name der Versammlungs-Subcollection unterhalb eines Kreis-Dokuments. */
internal const val CONGREGATION_SUBCOLLECTION = "congregation"

fun unavailableCircuitRepository(platform: String): CircuitRepository = object : CircuitRepository {
    override fun observeAll(): Flow<List<Circuit>> = flow {
        throw UnsupportedOperationException("Firestore is not configured for $platform.")
    }

    override suspend fun create(circuit: Circuit): Result<Circuit> =
        Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

    override suspend fun update(circuit: Circuit): Result<Circuit> =
        Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

    override suspend fun delete(circuitId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))
}
