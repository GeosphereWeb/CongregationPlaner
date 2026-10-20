package de.geosphere.congregationplaner.circuit.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.AndroidFirestoreProvider
import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.circuit.domain.model.CircuitException
import de.geosphere.congregationplaner.circuit.domain.repository.CircuitRepository
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.observeDocuments
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.firestore.stringField
import kotlinx.coroutines.flow.Flow

actual fun createCircuitRepository(): CircuitRepository = FirestoreCircuitRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreCircuitRepository(private val firestore: FirebaseFirestore) : CircuitRepository {
    override fun observeAll(): Flow<List<Circuit>> =
        observeDocuments(firestore.collection(CIRCUIT_COLLECTION_PATH), "circuit", ::toDomain)

    override suspend fun create(circuit: Circuit): Result<Circuit> = runCircuitCatching("create circuit") {
        require(circuit.id.isNotBlank()) { "A circuit ID is required to create a circuit." }
        val reference = firestore.collection(CIRCUIT_COLLECTION_PATH).document(circuit.id)
        // Die ID ist fachlich vergeben (Kreisnummer); ein bestehender Kreis darf nicht überschrieben werden.
        if (reference.get().awaitResult().exists()) throw CircuitException.AlreadyExists(circuit.id)
        reference.set(circuit.toFirestoreFields()).awaitResult()
        circuit
    }

    override suspend fun update(circuit: Circuit): Result<Circuit> = runCircuitCatching("update circuit") {
        require(circuit.id.isNotBlank()) { "A circuit ID is required to update a circuit." }
        firestore.collection(CIRCUIT_COLLECTION_PATH)
            .document(circuit.id)
            .update(circuit.toFirestoreFields())
            .awaitResult()
        circuit
    }

    override suspend fun delete(circuitId: String): Result<Unit> = runCircuitCatching("delete circuit") {
        require(circuitId.isNotBlank()) { "A circuit ID is required to delete a circuit." }
        val reference = firestore.collection(CIRCUIT_COLLECTION_PATH).document(circuitId)
        // Firestore löscht Subcollections nicht mit; ohne diese Prüfung blieben verwaiste Versammlungen zurück.
        val congregations = reference.collection(CONGREGATION_SUBCOLLECTION).limit(1).get().awaitResult()
        if (!congregations.isEmpty) throw CircuitException.HasCongregations(circuitId)
        reference.delete().awaitResult()
    }

    // Fachliche Fehler (CircuitException) werden nicht als Firestore-Fehler geloggt.
    private suspend fun <T> runCircuitCatching(operation: String, block: suspend () -> T): Result<T> =
        runFirestoreCatching(operation, isExpected = { it is CircuitException }, block = block)
}

private fun toDomain(document: DocumentSnapshot): Circuit = Circuit(
    id = document.id,
    name = document.stringField("name"),
    overseerName = document.stringField("overseer_name"),
)

private fun Circuit.toFirestoreFields(): Map<String, Any> = mapOf(
    "name" to name,
    "overseer_name" to overseerName,
)
