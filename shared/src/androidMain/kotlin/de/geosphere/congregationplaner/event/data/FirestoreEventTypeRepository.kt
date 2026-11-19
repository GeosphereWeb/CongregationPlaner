package de.geosphere.congregationplaner.event.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.data.AndroidFirestoreProvider
import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.event.domain.repository.EventTypeRepository
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.observeDocuments
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import kotlinx.coroutines.flow.Flow

actual fun createEventTypeRepository(): EventTypeRepository =
    FirestoreEventTypeRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreEventTypeRepository(private val firestore: FirebaseFirestore) : EventTypeRepository {
    override fun observeEventTypes(congregation: CongregationRef): Flow<List<EventType>> =
        observeDocuments(firestore.collection(congregation.eventTypesPath), "event type", ::toEventType)

    override suspend fun save(congregation: CongregationRef, type: EventType): Result<EventType> =
        runFirestoreCatching("save event type") {
            val collection = firestore.collection(congregation.eventTypesPath)
            // document() ohne Pfad vergibt eine zufällige ID.
            val document = if (type.id.isBlank()) collection.document() else collection.document(type.id)
            val saved = type.copy(id = document.id)
            document.set(mapOf("name" to saved.name, "cancelsTalk" to saved.cancelsTalk, "icon" to saved.icon))
                .awaitResult()
            saved
        }

    override suspend fun delete(congregation: CongregationRef, id: String): Result<Unit> =
        runFirestoreCatching("delete event type") {
            firestore.collection(congregation.eventTypesPath).document(id).delete().awaitResult()
        }
}

private fun toEventType(document: DocumentSnapshot): EventType = eventTypeOf(
    id = document.id,
    name = document.getString("name"),
    cancelsTalk = document.getBoolean("cancelsTalk"),
    icon = document.getString("icon"),
)
