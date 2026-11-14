package de.geosphere.congregationplaner.event.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.data.AndroidFirestoreProvider
import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.event.domain.model.SpecialEvent
import de.geosphere.congregationplaner.event.domain.repository.SpecialEventRepository
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.observeDocuments
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

actual fun createSpecialEventRepository(): SpecialEventRepository =
    FirestoreSpecialEventRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreSpecialEventRepository(private val firestore: FirebaseFirestore) : SpecialEventRepository {
    override fun observeAll(congregation: CongregationRef): Flow<List<SpecialEvent>> =
        observeDocuments(firestore.collection(congregation.eventsPath), "special event", ::toSpecialEvent)
            .map { it.filterNotNull() }

    override fun observeFor(congregation: CongregationRef, groups: Set<EventAudience>): Flow<List<SpecialEvent>> =
        observeDocuments(
            firestore.collection(congregation.eventsPath).whereArrayContainsAny(AUDIENCE_FIELD, groups.values()),
            "special event",
            ::toSpecialEvent,
        ).map { it.filterNotNull() }

    override suspend fun save(congregation: CongregationRef, event: SpecialEvent): Result<SpecialEvent> =
        runFirestoreCatching("save special event") {
            val collection = firestore.collection(congregation.eventsPath)
            // document() ohne Pfad vergibt eine zufällige ID.
            val document = if (event.id.isBlank()) collection.document() else collection.document(event.id)
            val saved = event.copy(id = document.id)
            document.set(
                mapOf(
                    "title" to saved.title,
                    "startDate" to saved.startDate.toString(),
                    "endDate" to saved.endDate.toString(),
                    AUDIENCE_FIELD to saved.audienceValues(),
                ),
            ).awaitResult()
            saved
        }

    override suspend fun delete(congregation: CongregationRef, id: String): Result<Unit> =
        runFirestoreCatching("delete special event") {
            firestore.collection(congregation.eventsPath).document(id).delete().awaitResult()
        }
}

private fun toSpecialEvent(document: DocumentSnapshot): SpecialEvent? = specialEventOf(
    id = document.id,
    title = document.getString("title"),
    startDate = document.getString("startDate"),
    endDate = document.getString("endDate"),
    audience = (document.get(AUDIENCE_FIELD) as? List<*>).orEmpty().map { it as? String },
)
