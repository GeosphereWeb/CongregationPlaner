package de.geosphere.congregationplaner.talk.data

import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.FirebaseAuthManager
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.firestoreBoolean
import de.geosphere.congregationplaner.firestore.firestoreFields
import de.geosphere.congregationplaner.firestore.firestoreString
import de.geosphere.congregationplaner.firestore.listAllDocuments
import de.geosphere.congregationplaner.firestore.optionalBooleanField
import de.geosphere.congregationplaner.firestore.optionalStringField
import de.geosphere.congregationplaner.firestore.pollingFlow
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import kotlinx.coroutines.flow.Flow
import java.net.http.HttpClient

internal class FirestoreRestTalkCatalogRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : TalkCatalogRepository {
    private val client = FirestoreRestClient(httpClient, idTokenProvider, "Sign in before accessing the talk list.")

    // Ein Dokument ohne gültige Nummer als ID wird übersprungen, damit es nicht die ganze Liste blockiert.
    override fun observeTalks(): Flow<List<Talk>> = pollingFlow {
        client.listAllDocuments(TALK_CATALOG_COLLECTION_PATH).mapNotNull { document ->
            document.id.toIntOrNull()?.takeIf { it >= 1 }?.let { number ->
                Talk(
                    number = number,
                    title = document.fields.optionalStringField("title").orEmpty(),
                    active = document.fields.optionalBooleanField("active") ?: true,
                )
            }
        }
    }

    override suspend fun save(talk: Talk): Result<Talk> = runCatchingIo {
        val body = firestoreFields(
            JsonObject().apply {
                add("title", firestoreString(talk.title))
                add("active", firestoreBoolean(talk.active))
            },
        )
        client.request("PATCH", client.documentUrl(TALK_CATALOG_COLLECTION_PATH, talk.number.toString()), body)
        talk
    }

    override suspend fun delete(number: Int): Result<Unit> = runCatchingIo {
        client.request("DELETE", client.documentUrl(TALK_CATALOG_COLLECTION_PATH, number.toString()))
    }.map {}
}
