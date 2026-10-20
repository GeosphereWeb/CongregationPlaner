package de.geosphere.congregationplaner.congregation.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.AndroidFirestoreProvider
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.observeDocuments
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.firestore.stringField
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime

actual fun createCongregationMetadataRepository(): CongregationMetadataRepository =
    FirestoreCongregationMetadataRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreCongregationMetadataRepository(private val firestore: FirebaseFirestore) :
    CongregationMetadataRepository {
    override fun observeAll(): Flow<List<CongregationMetadata>> =
        observeDocuments(firestore.collection(CONGREGATION_COLLECTION_PATH), "congregation", ::toDomain)

    override suspend fun save(congregation: CongregationMetadata): Result<CongregationMetadata> =
        runFirestoreCatching("save congregation") {
            val collection = firestore.collection(CONGREGATION_COLLECTION_PATH)
            val document = if (congregation.id.isBlank()) {
                collection.document().also { reference ->
                    reference.set(congregation.toFirestoreFields()).awaitResult()
                }
            } else {
                collection.document(congregation.id).also { reference ->
                    reference.update(congregation.toFirestoreFields()).awaitResult()
                }
            }
            congregation.copy(id = document.id)
        }

    override suspend fun delete(congregationId: String): Result<Unit> = runFirestoreCatching("delete congregation") {
        require(congregationId.isNotBlank()) { "A congregation ID is required to delete a congregation." }
        firestore.collection(CONGREGATION_COLLECTION_PATH)
            .document(congregationId)
            .delete()
            .awaitResult()
    }
}

private fun toDomain(document: DocumentSnapshot): CongregationMetadata = CongregationMetadata(
    id = document.id,
    name = document.stringField("name"),
    midweekMeetingTime = document.timeField("midweek_meeting_time"),
    midweekMeetingWeekday = document.intField("midweek_meeting_weekday"),
    weekendMeetingTime = document.timeField("weekend_meeting_time"),
    weekendMeetingWeekday = document.intField("weekend_meeting_weekday"),
)

private fun weekdayMessage(name: String) = "Firestore field '$name' must be an integer from 1 (Monday) to 7 (Sunday)."

private fun timeMessage(name: String) = "Firestore field '$name' must be an ISO time string such as '19:00'."

private fun DocumentSnapshot.intField(name: String): Int = when (val value = get(name)) {
    null -> 1
    is Number -> value.toInt().also { weekday ->
        require(value.toDouble() == weekday.toDouble() && weekday in 1..7) { weekdayMessage(name) }
    }
    else -> throw IllegalArgumentException(weekdayMessage(name))
}

private fun DocumentSnapshot.timeField(name: String): LocalTime = when (val value = get(name)) {
    null -> LocalTime(0, 0)
    is String -> try {
        LocalTime.parse(value)
    } catch (exception: IllegalArgumentException) {
        throw IllegalArgumentException(timeMessage(name), exception)
    }
    else -> throw IllegalArgumentException(timeMessage(name))
}

private fun CongregationMetadata.toFirestoreFields(): Map<String, Any> = mapOf(
    "name" to name,
    "midweek_meeting_time" to midweekMeetingTime.toString(),
    "midweek_meeting_weekday" to midweekMeetingWeekday,
    "weekend_meeting_time" to weekendMeetingTime.toString(),
    "weekend_meeting_weekday" to weekendMeetingWeekday,
)
