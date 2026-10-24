package de.geosphere.congregationplaner.talk.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.AndroidFirestoreProvider
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.logFirestoreFailure
import de.geosphere.congregationplaner.firestore.observeDocuments
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

actual fun createTalkCatalogRepository(): TalkCatalogRepository =
    FirestoreTalkCatalogRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreTalkCatalogRepository(private val firestore: FirebaseFirestore) : TalkCatalogRepository {
    override fun observeTalks(): Flow<List<Talk>> =
        observeDocuments(firestore.collection(TALK_CATALOG_COLLECTION_PATH), "talk", ::toTalkOrNull)
            .map { it.filterNotNull() }

    override suspend fun save(talk: Talk): Result<Talk> = runFirestoreCatching("save talk") {
        firestore.collection(TALK_CATALOG_COLLECTION_PATH).document(talk.number.toString())
            .set(mapOf("title" to talk.title, "active" to talk.active))
            .awaitResult()
        talk
    }

    override suspend fun delete(number: Int): Result<Unit> = runFirestoreCatching("delete talk") {
        firestore.collection(TALK_CATALOG_COLLECTION_PATH).document(number.toString()).delete().awaitResult()
    }
}

// Ein Dokument ohne gültige Nummer als ID wird übersprungen, damit es nicht die ganze Liste blockiert.
private fun toTalkOrNull(document: DocumentSnapshot): Talk? {
    val number = document.id.toIntOrNull()?.takeIf { it >= 1 }
    if (number == null) {
        logFirestoreFailure("parse talk ${document.id}", IllegalArgumentException("Talk id must be a number."))
        return null
    }
    return Talk(
        number = number,
        title = document.getString("title").orEmpty(),
        active = document.getBoolean("active") ?: true,
    )
}
