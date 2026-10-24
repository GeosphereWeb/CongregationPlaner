package de.geosphere.congregationplaner.talk.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.AndroidFirestoreProvider
import de.geosphere.congregationplaner.firestore.observeDocuments
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import kotlinx.coroutines.flow.Flow

actual fun createWeekEventRepository(): WeekEventRepository =
    FirestoreWeekEventRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreWeekEventRepository(private val firestore: FirebaseFirestore) : WeekEventRepository {
    override fun observeWeekEvents(): Flow<List<WeekEvent>> =
        observeDocuments(firestore.collection(WEEK_EVENTS_COLLECTION_PATH), "week event", ::toWeekEvent)
}

private fun toWeekEvent(document: DocumentSnapshot): WeekEvent = WeekEvent(
    id = document.id,
    name = document.getString("name").orEmpty(),
    cancelsTalk = document.getBoolean("cancelsTalk") ?: false,
)
