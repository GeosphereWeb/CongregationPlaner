package de.geosphere.congregationplaner.event.data

import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.data.FirebaseAuthManager
import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.event.domain.repository.EventTypeRepository
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.FirestoreRestDocument
import de.geosphere.congregationplaner.firestore.booleanField
import de.geosphere.congregationplaner.firestore.firestoreBoolean
import de.geosphere.congregationplaner.firestore.firestoreFields
import de.geosphere.congregationplaner.firestore.firestoreString
import de.geosphere.congregationplaner.firestore.listAllDocuments
import de.geosphere.congregationplaner.firestore.optionalStringField
import de.geosphere.congregationplaner.firestore.parseDocument
import de.geosphere.congregationplaner.firestore.pollingFlow
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import kotlinx.coroutines.flow.Flow
import java.net.http.HttpClient

actual fun createEventTypeRepository(): EventTypeRepository = FirestoreRestEventTypeRepository()

internal class FirestoreRestEventTypeRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : EventTypeRepository {
    private val client = FirestoreRestClient(httpClient, idTokenProvider, "Sign in before accessing event types.")

    override fun observeEventTypes(congregation: CongregationRef): Flow<List<EventType>> = pollingFlow {
        client.listAllDocuments(congregation.eventTypesPath).map { it.toEventType() }
    }

    override suspend fun save(congregation: CongregationRef, type: EventType): Result<EventType> = runCatchingIo {
        val body = firestoreFields(
            JsonObject().apply {
                add("name", firestoreString(type.name))
                add("cancelsTalk", firestoreBoolean(type.cancelsTalk))
                add("icon", firestoreString(type.icon))
            },
        )
        if (type.id.isBlank()) {
            // Ohne documentId vergibt Firestore eine zufällige ID.
            val created = parseDocument(client.request("POST", client.collectionUrl(congregation.eventTypesPath), body))
            type.copy(id = created.id)
        } else {
            // PATCH ohne updateMask ersetzt das Dokument.
            client.request("PATCH", client.documentUrl(congregation.eventTypesPath, type.id), body)
            type
        }
    }

    override suspend fun delete(congregation: CongregationRef, id: String): Result<Unit> = runCatchingIo {
        client.request("DELETE", client.documentUrl(congregation.eventTypesPath, id))
    }.map {}
}

private fun FirestoreRestDocument.toEventType(): EventType = eventTypeOf(
    id = id,
    name = fields.optionalStringField("name"),
    cancelsTalk = fields.booleanField("cancelsTalk"),
    icon = fields.optionalStringField("icon"),
)
