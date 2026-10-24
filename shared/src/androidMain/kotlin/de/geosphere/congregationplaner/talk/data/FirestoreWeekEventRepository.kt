package de.geosphere.congregationplaner.talk.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.AndroidFirestoreProvider
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.observeDocuments
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import kotlinx.coroutines.flow.Flow

actual fun createWeekEventRepository(): WeekEventRepository =
    FirestoreWeekEventRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreWeekEventRepository(private val firestore: FirebaseFirestore) : WeekEventRepository {
    override fun observeWeekEvents(): Flow<List<WeekEvent>> =
        observeDocuments(firestore.collection(WEEK_EVENTS_COLLECTION_PATH), "week event", ::toWeekEvent)

    override suspend fun save(event: WeekEvent): Result<WeekEvent> = runFirestoreCatching("save week event") {
        val collection = firestore.collection(WEEK_EVENTS_COLLECTION_PATH)
        // document() ohne Pfad vergibt eine zufällige ID.
        val document = if (event.id.isBlank()) collection.document() else collection.document(event.id)
        val saved = event.copy(id = document.id)
        document.set(mapOf("name" to saved.name, "cancelsTalk" to saved.cancelsTalk)).awaitResult()
        saved
    }

    override suspend fun delete(id: String): Result<Unit> = runFirestoreCatching("delete week event") {
        firestore.collection(WEEK_EVENTS_COLLECTION_PATH).document(id).delete().awaitResult()
    }
}

private fun toWeekEvent(document: DocumentSnapshot): WeekEvent = WeekEvent(
    id = document.id,
    name = document.getString("name").orEmpty(),
    cancelsTalk = document.getBoolean("cancelsTalk") ?: false,
)
