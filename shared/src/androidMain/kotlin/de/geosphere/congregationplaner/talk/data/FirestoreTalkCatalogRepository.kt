package de.geosphere.congregationplaner.talk.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.data.AndroidFirestoreProvider
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.logFirestoreFailure
import de.geosphere.congregationplaner.firestore.observeDocuments
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.organization.domain.model.CountryRef
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

actual fun createTalkCatalogRepository(): TalkCatalogRepository =
    FirestoreTalkCatalogRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreTalkCatalogRepository(private val firestore: FirebaseFirestore) : TalkCatalogRepository {
    override fun observeTalks(country: CountryRef): Flow<List<Talk>> =
        observeDocuments(firestore.collection(country.talkCatalogPath), "talk", ::toTalkOrNull)
            .map { it.filterNotNull() }

    override suspend fun save(country: CountryRef, talk: Talk): Result<Talk> = runFirestoreCatching("save talk") {
        firestore.collection(country.talkCatalogPath).document(talk.number.toString())
            .set(
                mapOf(
                    "title" to talk.title,
                    "active" to talk.active,
                    "titleChanges" to talk.titleChanges.map {
                        mapOf("validFrom" to it.validFrom.toString(), "title" to it.title)
                    },
                ),
            )
            .awaitResult()
        talk
    }

    override suspend fun delete(country: CountryRef, number: Int): Result<Unit> = runFirestoreCatching("delete talk") {
        firestore.collection(country.talkCatalogPath).document(number.toString()).delete().awaitResult()
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
        titleChanges = (document.get("titleChanges") as? List<*>).orEmpty().mapNotNull { change ->
            (change as? Map<*, *>)?.let { titleChangeOrNull(it["validFrom"] as? String, it["title"] as? String) }
        },
    )
}
