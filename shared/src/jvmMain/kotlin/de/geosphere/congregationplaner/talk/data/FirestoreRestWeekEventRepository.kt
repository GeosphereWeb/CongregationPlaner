package de.geosphere.congregationplaner.talk.data

import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.data.FirebaseAuthManager
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
import de.geosphere.congregationplaner.organization.domain.model.CountryRef
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import kotlinx.coroutines.flow.Flow
import java.net.http.HttpClient

internal class FirestoreRestWeekEventRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : WeekEventRepository {
    private val client = FirestoreRestClient(httpClient, idTokenProvider, "Sign in before accessing week events.")

    override fun observeWeekEvents(country: CountryRef): Flow<List<WeekEvent>> = pollingFlow {
        client.listAllDocuments(country.weekEventsPath).map { it.toWeekEvent() }
    }

    override suspend fun save(country: CountryRef, event: WeekEvent): Result<WeekEvent> = runCatchingIo {
        val body = firestoreFields(
            JsonObject().apply {
                add("name", firestoreString(event.name))
                add("cancelsTalk", firestoreBoolean(event.cancelsTalk))
            },
        )
        if (event.id.isBlank()) {
            // Ohne documentId vergibt Firestore eine zufällige ID.
            parseDocument(client.request("POST", client.collectionUrl(country.weekEventsPath), body)).toWeekEvent()
        } else {
            client.request("PATCH", client.documentUrl(country.weekEventsPath, event.id), body)
            event
        }
    }

    override suspend fun delete(country: CountryRef, id: String): Result<Unit> = runCatchingIo {
        client.request("DELETE", client.documentUrl(country.weekEventsPath, id))
    }.map {}

    private fun FirestoreRestDocument.toWeekEvent(): WeekEvent = WeekEvent(
        id = id,
        name = fields.optionalStringField("name").orEmpty(),
        cancelsTalk = fields.booleanField("cancelsTalk"),
    )
}
