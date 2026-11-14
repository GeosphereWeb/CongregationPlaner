package de.geosphere.congregationplaner.event.data

import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.data.FirebaseAuthManager
import de.geosphere.congregationplaner.event.domain.model.SpecialEvent
import de.geosphere.congregationplaner.event.domain.repository.SpecialEventRepository
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.FirestoreRestDocument
import de.geosphere.congregationplaner.firestore.firestoreFields
import de.geosphere.congregationplaner.firestore.firestoreString
import de.geosphere.congregationplaner.firestore.firestoreStringArray
import de.geosphere.congregationplaner.firestore.listAllDocuments
import de.geosphere.congregationplaner.firestore.optionalStringField
import de.geosphere.congregationplaner.firestore.parseDocument
import de.geosphere.congregationplaner.firestore.pollingFlow
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.firestore.stringArrayField
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import kotlinx.coroutines.flow.Flow
import java.net.http.HttpClient

actual fun createSpecialEventRepository(): SpecialEventRepository = FirestoreRestSpecialEventRepository()

internal class FirestoreRestSpecialEventRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : SpecialEventRepository {
    private val client = FirestoreRestClient(httpClient, idTokenProvider, "Sign in before accessing special events.")

    override fun observeAll(congregation: CongregationRef): Flow<List<SpecialEvent>> = pollingFlow {
        client.listAllDocuments(congregation.eventsPath).mapNotNull { it.toSpecialEvent() }
    }

    override suspend fun save(congregation: CongregationRef, event: SpecialEvent): Result<SpecialEvent> =
        runCatchingIo {
            val body = firestoreFields(
                JsonObject().apply {
                    add("title", firestoreString(event.title))
                    add("startDate", firestoreString(event.startDate.toString()))
                    add("endDate", firestoreString(event.endDate.toString()))
                    add("audience", firestoreStringArray(event.audienceValues()))
                },
            )
            if (event.id.isBlank()) {
                // Ohne documentId vergibt Firestore eine zufällige ID.
                val created = parseDocument(client.request("POST", client.collectionUrl(congregation.eventsPath), body))
                event.copy(id = created.id)
            } else {
                // PATCH ohne updateMask ersetzt das Dokument.
                client.request("PATCH", client.documentUrl(congregation.eventsPath, event.id), body)
                event
            }
        }

    override suspend fun delete(congregation: CongregationRef, id: String): Result<Unit> = runCatchingIo {
        client.request("DELETE", client.documentUrl(congregation.eventsPath, id))
    }.map {}
}

private fun FirestoreRestDocument.toSpecialEvent(): SpecialEvent? = specialEventOf(
    id = id,
    title = fields.optionalStringField("title"),
    startDate = fields.optionalStringField("startDate"),
    endDate = fields.optionalStringField("endDate"),
    audience = fields.stringArrayField("audience"),
)
