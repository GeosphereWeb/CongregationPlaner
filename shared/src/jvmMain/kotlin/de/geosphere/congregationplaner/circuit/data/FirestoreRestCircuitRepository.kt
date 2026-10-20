package de.geosphere.congregationplaner.circuit.data

import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.FirebaseAuthManager
import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.circuit.domain.model.CircuitException
import de.geosphere.congregationplaner.circuit.domain.repository.CircuitRepository
import de.geosphere.congregationplaner.firestore.FirestoreHttpException
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.FirestoreRestDocument
import de.geosphere.congregationplaner.firestore.encode
import de.geosphere.congregationplaner.firestore.firestoreFields
import de.geosphere.congregationplaner.firestore.firestoreString
import de.geosphere.congregationplaner.firestore.parseDocuments
import de.geosphere.congregationplaner.firestore.pollingFlow
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.firestore.stringField
import kotlinx.coroutines.flow.Flow
import java.net.http.HttpClient

internal class FirestoreRestCircuitRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : CircuitRepository {
    private val client = FirestoreRestClient(httpClient, idTokenProvider, "Sign in before accessing circuit data.")

    override fun observeAll(): Flow<List<Circuit>> = pollingFlow {
        parseDocuments(client.request("GET", "${client.collectionUrl(CIRCUIT_COLLECTION_PATH)}?pageSize=1000"))
            .map { it.toCircuit() }
    }

    override suspend fun create(circuit: Circuit): Result<Circuit> = runCatchingIo {
        require(circuit.id.isNotBlank()) { "A circuit ID is required to create a circuit." }
        try {
            // POST mit documentId schlägt mit 409 fehl, falls die Kreisnummer bereits vergeben ist.
            client.request(
                method = "POST",
                url = "${client.collectionUrl(CIRCUIT_COLLECTION_PATH)}?documentId=${encode(circuit.id)}",
                body = circuit.toFirestoreJson(),
            )
        } catch (exception: FirestoreHttpException) {
            if (exception.status == HTTP_CONFLICT) throw CircuitException.AlreadyExists(circuit.id)
            throw exception
        }
        circuit
    }

    override suspend fun update(circuit: Circuit): Result<Circuit> = runCatchingIo {
        require(circuit.id.isNotBlank()) { "A circuit ID is required to update a circuit." }
        // currentDocument.exists verhindert, dass ein PATCH auf einen gelöschten Kreis ihn neu anlegt.
        client.request(
            method = "PATCH",
            url = "${documentUrl(circuit.id)}?${client.updateMask(FIRESTORE_FIELD_NAMES)}&currentDocument.exists=true",
            body = circuit.toFirestoreJson(),
        )
        circuit
    }

    override suspend fun delete(circuitId: String): Result<Unit> = runCatchingIo {
        require(circuitId.isNotBlank()) { "A circuit ID is required to delete a circuit." }
        // Firestore löscht Subcollections nicht mit; ohne diese Prüfung blieben verwaiste Versammlungen zurück.
        val congregations = client.request("GET", "${documentUrl(circuitId)}/$CONGREGATION_SUBCOLLECTION?pageSize=1")
        if (parseDocuments(congregations).isNotEmpty()) throw CircuitException.HasCongregations(circuitId)
        client.request("DELETE", documentUrl(circuitId))
    }

    private fun documentUrl(id: String): String = client.documentUrl(CIRCUIT_COLLECTION_PATH, id)

    private fun FirestoreRestDocument.toCircuit(): Circuit = Circuit(
        id = id,
        name = fields.stringField("name"),
        overseerName = fields.stringField("overseer_name"),
    )

    private fun Circuit.toFirestoreJson(): String = firestoreFields(
        JsonObject().apply {
            add("name", firestoreString(name))
            add("overseer_name", firestoreString(overseerName))
        },
    )

    private companion object {
        const val HTTP_CONFLICT = 409
        val FIRESTORE_FIELD_NAMES = listOf("name", "overseer_name")
    }
}
