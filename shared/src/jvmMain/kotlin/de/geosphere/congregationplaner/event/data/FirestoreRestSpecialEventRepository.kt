package de.geosphere.congregationplaner.event.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.geosphere.congregationplaner.auth.data.FirebaseAuthManager
import de.geosphere.congregationplaner.event.domain.model.EventAudience
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

    override fun observeFor(congregation: CongregationRef, groups: Set<EventAudience>): Flow<List<SpecialEvent>> =
        pollingFlow {
            val url = "${client.collectionUrl(congregation.documentPath)}:runQuery"
            // Each result row holds a document, except the row that only reports the read time.
            JsonParser.parseString(client.request("POST", url, audienceQuery(groups)))
                .asJsonArray
                .mapNotNull { row -> row.asJsonObject.getAsJsonObject("document") }
                .mapNotNull { parseDocument(it).toSpecialEvent() }
        }

    override suspend fun save(congregation: CongregationRef, event: SpecialEvent): Result<SpecialEvent> =
        runCatchingIo {
            val body = firestoreFields(
                JsonObject().apply {
                    add("title", firestoreString(event.title))
                    add("startDate", firestoreString(event.startDate.toString()))
                    add("endDate", firestoreString(event.endDate.toString()))
                    add(AUDIENCE_FIELD, firestoreStringArray(event.audienceValues()))
                    event.eventTypeId?.let { add(EVENT_TYPE_FIELD, firestoreString(it)) }
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

/** Strukturierte Abfrage: Ereignisse der Collection, deren `audience` eine der [groups] enthält. */
private fun audienceQuery(groups: Set<EventAudience>): String {
    val filter = JsonObject().apply {
        add("field", JsonObject().apply { addProperty("fieldPath", AUDIENCE_FIELD) })
        addProperty("op", "ARRAY_CONTAINS_ANY")
        add("value", firestoreStringArray(groups.values()))
    }
    val query = JsonObject().apply {
        add("from", JsonArray().apply { add(JsonObject().apply { addProperty("collectionId", EVENTS_SUBCOLLECTION) }) })
        add("where", JsonObject().apply { add("fieldFilter", filter) })
    }
    return JsonObject().apply { add("structuredQuery", query) }.toString()
}

private fun FirestoreRestDocument.toSpecialEvent(): SpecialEvent? = specialEventOf(
    id = id,
    title = fields.optionalStringField("title"),
    startDate = fields.optionalStringField("startDate"),
    endDate = fields.optionalStringField("endDate"),
    audience = fields.stringArrayField(AUDIENCE_FIELD),
    eventTypeId = fields.optionalStringField(EVENT_TYPE_FIELD),
)
