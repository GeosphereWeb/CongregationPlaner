package de.geosphere.congregationplaner.congregation.data

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.FirebaseAndroidContextHolder
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual fun createCongregationMetadataRepository(): CongregationMetadataRepository {
    val firestore = FirebaseFirestore.getInstance()
    if (FirebaseAndroidContextHolder.useAuthEmulator) {
        firestore.useEmulator(FirebaseAndroidContextHolder.authEmulatorHost, FIRESTORE_EMULATOR_PORT)
    }
    return FirestoreCongregationMetadataRepository(firestore)
}

private class FirestoreCongregationMetadataRepository(private val firestore: FirebaseFirestore) :
    CongregationMetadataRepository {
    override fun observeAll(): Flow<List<CongregationMetadata>> = callbackFlow {
        val registration = firestore.collection(CONGREGATION_COLLECTION_PATH)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    close(IllegalStateException("Firestore returned an empty congregation snapshot."))
                    return@addSnapshotListener
                }
                val result = runCatching { snapshot.documents.map(::toDomain) }
                result.onSuccess { trySend(it) }
                    .onFailure { close(it) }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun save(congregation: CongregationMetadata): Result<CongregationMetadata> = try {
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
        Result.success(congregation.copy(id = document.id))
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}

private fun toDomain(document: com.google.firebase.firestore.DocumentSnapshot): CongregationMetadata =
    CongregationMetadata(
        id = document.id,
        name = document.stringField("name"),
        midweekMeetingTime = document.stringField("midweek_meeting_time"),
        midweekMeetingWeekday = document.stringField("midweek_meeting_weekday"),
        weekendMeetingTime = document.stringField("weekend_meeting_time"),
        weekendMeetingWeekday = document.stringField("weekend_meeting_weekday"),
    )

private fun com.google.firebase.firestore.DocumentSnapshot.stringField(name: String): String =
    when (val value = get(name)) {
        null -> ""
        is String -> value
        else -> throw IllegalArgumentException("Firestore field '$name' must be a string.")
    }

private fun CongregationMetadata.toFirestoreFields(): Map<String, String> = mapOf(
    "name" to name,
    "midweek_meeting_time" to midweekMeetingTime,
    "midweek_meeting_weekday" to midweekMeetingWeekday,
    "weekend_meeting_time" to weekendMeetingTime,
    "weekend_meeting_weekday" to weekendMeetingWeekday,
)

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { value ->
        if (continuation.isActive) continuation.resume(value)
    }
    addOnFailureListener { exception ->
        if (continuation.isActive) continuation.resumeWithException(exception)
    }
}

private const val FIRESTORE_EMULATOR_PORT = 8080
