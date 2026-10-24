package de.geosphere.congregationplaner.talk.data

import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.FirebaseAuthManager
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.FirestoreRestDocument
import de.geosphere.congregationplaner.firestore.firestoreFields
import de.geosphere.congregationplaner.firestore.firestoreString
import de.geosphere.congregationplaner.firestore.listAllDocuments
import de.geosphere.congregationplaner.firestore.optionalStringField
import de.geosphere.congregationplaner.firestore.parseDocument
import de.geosphere.congregationplaner.firestore.pollingFlow
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerRepository
import kotlinx.coroutines.flow.Flow
import java.net.http.HttpClient

internal class FirestoreRestExternalSpeakerRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : ExternalSpeakerRepository {
    private val client =
        FirestoreRestClient(httpClient, idTokenProvider, "Sign in before accessing external speakers.")

    override fun observeAll(congregation: CongregationRef): Flow<List<ExternalSpeaker>> = pollingFlow {
        client.listAllDocuments(congregation.externalSpeakersPath).map { it.toExternalSpeaker() }
    }

    override suspend fun save(congregation: CongregationRef, speaker: ExternalSpeaker): Result<ExternalSpeaker> =
        runCatchingIo {
            val path = congregation.externalSpeakersPath
            if (speaker.id.isBlank()) {
                // Ohne documentId vergibt Firestore eine zufällige ID.
                parseDocument(client.request("POST", client.collectionUrl(path), speaker.toBody())).toExternalSpeaker()
            } else {
                client.request("PATCH", client.documentUrl(path, speaker.id), speaker.toBody())
                speaker
            }
        }

    override suspend fun delete(congregation: CongregationRef, id: String): Result<Unit> = runCatchingIo {
        client.request("DELETE", client.documentUrl(congregation.externalSpeakersPath, id))
    }.map {}

    private fun FirestoreRestDocument.toExternalSpeaker(): ExternalSpeaker = ExternalSpeaker(
        id = id,
        lastName = fields.optionalStringField("lastName").orEmpty(),
        firstName = fields.optionalStringField("firstName").orEmpty(),
        congregationName = fields.optionalStringField("congregationName").orEmpty(),
    )

    private fun ExternalSpeaker.toBody(): String = firestoreFields(
        JsonObject().apply {
            add("lastName", firestoreString(lastName))
            add("firstName", firestoreString(firstName))
            add("congregationName", firestoreString(congregationName))
        },
    )
}
