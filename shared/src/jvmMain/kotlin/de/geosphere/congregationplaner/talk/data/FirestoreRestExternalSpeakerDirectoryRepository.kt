package de.geosphere.congregationplaner.talk.data

import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.FirebaseAuthManager
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.FirestoreRestDocument
import de.geosphere.congregationplaner.firestore.firestoreFields
import de.geosphere.congregationplaner.firestore.firestoreIntegerArray
import de.geosphere.congregationplaner.firestore.firestoreString
import de.geosphere.congregationplaner.firestore.intArrayField
import de.geosphere.congregationplaner.firestore.listAllDocuments
import de.geosphere.congregationplaner.firestore.optionalStringField
import de.geosphere.congregationplaner.firestore.parseDocument
import de.geosphere.congregationplaner.firestore.pollingFlow
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerDirectoryRepository
import kotlinx.coroutines.flow.Flow
import java.net.http.HttpClient

internal class FirestoreRestExternalSpeakerDirectoryRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : ExternalSpeakerDirectoryRepository {
    private val client =
        FirestoreRestClient(httpClient, idTokenProvider, "Sign in before accessing external speakers.")

    override fun observeCircuits(congregation: CongregationRef): Flow<List<ExternalCircuit>> = pollingFlow {
        client.listAllDocuments(congregation.externalCircuitsPath).map {
            ExternalCircuit(id = it.id, name = it.fields.optionalStringField("name").orEmpty())
        }
    }

    override fun observeCongregations(congregation: CongregationRef): Flow<List<ExternalCongregation>> = pollingFlow {
        client.listAllDocuments(congregation.externalCongregationsPath).map {
            ExternalCongregation(
                id = it.id,
                circuitId = it.fields.optionalStringField("circuitId").orEmpty(),
                name = it.fields.optionalStringField("name").orEmpty(),
            )
        }
    }

    override fun observeSpeakers(congregation: CongregationRef): Flow<List<VisitingSpeaker>> = pollingFlow {
        client.listAllDocuments(congregation.visitingSpeakersPath).map { it.toVisitingSpeaker() }
    }

    override suspend fun saveCircuit(congregation: CongregationRef, circuit: ExternalCircuit) = store(
        congregation.externalCircuitsPath,
        circuit.id,
        JsonObject().apply { add("name", firestoreString(circuit.name)) },
    ) { circuit.copy(id = it) }

    override suspend fun saveCongregation(congregation: CongregationRef, externalCongregation: ExternalCongregation) =
        store(
            congregation.externalCongregationsPath,
            externalCongregation.id,
            JsonObject().apply {
                add("circuitId", firestoreString(externalCongregation.circuitId))
                add("name", firestoreString(externalCongregation.name))
            },
        ) { externalCongregation.copy(id = it) }

    override suspend fun saveSpeaker(congregation: CongregationRef, speaker: VisitingSpeaker) = store(
        congregation.visitingSpeakersPath,
        speaker.id,
        JsonObject().apply {
            add("congregationId", firestoreString(speaker.congregationId))
            add("lastName", firestoreString(speaker.lastName))
            add("firstName", firestoreString(speaker.firstName))
            add("phone", firestoreString(speaker.phone))
            add("email", firestoreString(speaker.email))
            add("talkNumbers", firestoreIntegerArray(speaker.talkNumbers))
        },
    ) { speaker.copy(id = it) }

    override suspend fun deleteCircuit(congregation: CongregationRef, id: String) =
        remove(congregation.externalCircuitsPath, id)

    override suspend fun deleteCongregation(congregation: CongregationRef, id: String) =
        remove(congregation.externalCongregationsPath, id)

    override suspend fun deleteSpeaker(congregation: CongregationRef, id: String) =
        remove(congregation.visitingSpeakersPath, id)

    private suspend fun <T> store(path: String, id: String, fields: JsonObject, withId: (String) -> T): Result<T> =
        runCatchingIo {
            val body = firestoreFields(fields)
            if (id.isBlank()) {
                // Ohne documentId vergibt Firestore eine zufällige ID.
                withId(parseDocument(client.request("POST", client.collectionUrl(path), body)).id)
            } else {
                client.request("PATCH", client.documentUrl(path, id), body)
                withId(id)
            }
        }

    private suspend fun remove(path: String, id: String): Result<Unit> =
        runCatchingIo { client.request("DELETE", client.documentUrl(path, id)) }.map {}

    private fun FirestoreRestDocument.toVisitingSpeaker(): VisitingSpeaker = VisitingSpeaker(
        id = id,
        congregationId = fields.optionalStringField("congregationId").orEmpty(),
        lastName = fields.optionalStringField("lastName").orEmpty(),
        firstName = fields.optionalStringField("firstName").orEmpty(),
        phone = fields.optionalStringField("phone").orEmpty(),
        email = fields.optionalStringField("email").orEmpty(),
        talkNumbers = fields.intArrayField("talkNumbers"),
    )
}
