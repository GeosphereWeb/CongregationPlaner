package de.geosphere.congregationplaner.talk.data

import de.geosphere.congregationplaner.auth.FirebaseAuthManager
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.FirestoreRestDocument
import de.geosphere.congregationplaner.firestore.booleanField
import de.geosphere.congregationplaner.firestore.optionalStringField
import de.geosphere.congregationplaner.firestore.parseDocuments
import de.geosphere.congregationplaner.firestore.pollingFlow
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import kotlinx.coroutines.flow.Flow
import java.net.http.HttpClient

internal class FirestoreRestWeekEventRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : WeekEventRepository {
    private val client = FirestoreRestClient(httpClient, idTokenProvider, "Sign in before accessing week events.")

    override fun observeWeekEvents(): Flow<List<WeekEvent>> = pollingFlow {
        parseDocuments(client.request("GET", "${client.collectionUrl(WEEK_EVENTS_COLLECTION_PATH)}?pageSize=200"))
            .map { it.toWeekEvent() }
    }

    private fun FirestoreRestDocument.toWeekEvent(): WeekEvent = WeekEvent(
        id = id,
        name = fields.optionalStringField("name").orEmpty(),
        cancelsTalk = fields.booleanField("cancelsTalk"),
    )
}
