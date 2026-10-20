package de.geosphere.congregationplaner.circuit.data

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.AndroidFirestoreProvider
import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.circuit.domain.model.CircuitException
import de.geosphere.congregationplaner.circuit.domain.repository.CircuitRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual fun createCircuitRepository(): CircuitRepository = FirestoreCircuitRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreCircuitRepository(private val firestore: FirebaseFirestore) : CircuitRepository {
    override fun observeAll(): Flow<List<Circuit>> = callbackFlow {
        val registration = firestore.collection(CIRCUIT_COLLECTION_PATH)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logFirestoreFailure("observe circuits", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    val exception = IllegalStateException("Firestore returned an empty circuit snapshot.")
                    logFirestoreFailure("observe circuits", exception)
                    close(exception)
                    return@addSnapshotListener
                }
                val result = runCatching { snapshot.documents.map(::toDomain) }
                result.onSuccess { trySend(it) }
                    .onFailure {
                        logFirestoreFailure("parse circuit snapshot", it)
                        close(it)
                    }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun create(circuit: Circuit): Result<Circuit> = runFirestoreCatching("create circuit") {
        require(circuit.id.isNotBlank()) { "A circuit ID is required to create a circuit." }
        val reference = firestore.collection(CIRCUIT_COLLECTION_PATH).document(circuit.id)
        // Die ID ist fachlich vergeben (Kreisnummer); ein bestehender Kreis darf nicht überschrieben werden.
        if (reference.get().awaitResult().exists()) throw CircuitException.AlreadyExists(circuit.id)
        reference.set(circuit.toFirestoreFields()).awaitResult()
        circuit
    }

    override suspend fun update(circuit: Circuit): Result<Circuit> = runFirestoreCatching("update circuit") {
        require(circuit.id.isNotBlank()) { "A circuit ID is required to update a circuit." }
        firestore.collection(CIRCUIT_COLLECTION_PATH)
            .document(circuit.id)
            .update(circuit.toFirestoreFields())
            .awaitResult()
        circuit
    }

    override suspend fun delete(circuitId: String): Result<Unit> = runFirestoreCatching("delete circuit") {
        require(circuitId.isNotBlank()) { "A circuit ID is required to delete a circuit." }
        val reference = firestore.collection(CIRCUIT_COLLECTION_PATH).document(circuitId)
        // Firestore löscht Subcollections nicht mit; ohne diese Prüfung blieben verwaiste Versammlungen zurück.
        val congregations = reference.collection(CONGREGATION_SUBCOLLECTION).limit(1).get().awaitResult()
        if (!congregations.isEmpty) throw CircuitException.HasCongregations(circuitId)
        reference.delete().awaitResult()
        Unit
    }

    private suspend fun <T> runFirestoreCatching(operation: String, block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: CircuitException) {
        Result.failure(exception)
    } catch (exception: Exception) {
        logFirestoreFailure(operation, exception)
        Result.failure(exception)
    }
}

private fun logFirestoreFailure(operation: String, exception: Throwable) {
    Log.e(FIRESTORE_LOG_TAG, "Failed to $operation.", exception)
}

private fun toDomain(document: DocumentSnapshot): Circuit = Circuit(
    id = document.id,
    name = document.stringField("name"),
    overseerName = document.stringField("overseer_name"),
)

private fun DocumentSnapshot.stringField(name: String): String = when (val value = get(name)) {
    null -> ""
    is String -> value
    else -> throw IllegalArgumentException("Firestore field '$name' must be a string.")
}

private fun Circuit.toFirestoreFields(): Map<String, Any> = mapOf(
    "name" to name,
    "overseer_name" to overseerName,
)

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { value ->
        if (continuation.isActive) continuation.resume(value)
    }
    addOnFailureListener { exception ->
        if (continuation.isActive) continuation.resumeWithException(exception)
    }
}

private const val FIRESTORE_LOG_TAG = "FirebaseFirestore"
