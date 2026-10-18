package de.geosphere.congregationplaner.congregation.data

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.AndroidFirestoreProvider
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import kotlinx.datetime.LocalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual fun createCongregationMetadataRepository(): CongregationMetadataRepository =
    FirestoreCongregationMetadataRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreCongregationMetadataRepository(private val firestore: FirebaseFirestore) :
    CongregationMetadataRepository {
    override fun observeAll(): Flow<List<CongregationMetadata>> = callbackFlow {
        val registration = firestore.collection(CONGREGATION_COLLECTION_PATH)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logFirestoreFailure("observe congregations", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    val exception = IllegalStateException("Firestore returned an empty congregation snapshot.")
                    logFirestoreFailure("observe congregations", exception)
                    close(exception)
                    return@addSnapshotListener
                }
                val result = runCatching { snapshot.documents.map(::toDomain) }
                result.onSuccess { trySend(it) }
                    .onFailure {
                        logFirestoreFailure("parse congregation snapshot", it)
                        close(it)
                    }
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
        logFirestoreFailure("save congregation", exception)
        Result.failure(exception)
    }

    override suspend fun delete(congregationId: String): Result<Unit> = try {
        require(congregationId.isNotBlank()) { "A congregation ID is required to delete a congregation." }
        firestore.collection(CONGREGATION_COLLECTION_PATH)
            .document(congregationId)
            .delete()
            .awaitResult()
        Result.success(Unit)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        logFirestoreFailure("delete congregation", exception)
        Result.failure(exception)
    }
}

private fun logFirestoreFailure(operation: String, exception: Throwable) {
    Log.e(FIRESTORE_LOG_TAG, "Failed to $operation.", exception)
}

private fun toDomain(document: com.google.firebase.firestore.DocumentSnapshot): CongregationMetadata =
    CongregationMetadata(
        id = document.id,
        name = document.stringField("name"),
        midweekMeetingTime = document.timeField("midweek_meeting_time"),
        midweekMeetingWeekday = document.intField("midweek_meeting_weekday"),
        weekendMeetingTime = document.timeField("weekend_meeting_time"),
        weekendMeetingWeekday = document.intField("weekend_meeting_weekday"),
    )

private fun com.google.firebase.firestore.DocumentSnapshot.intField(name: String): Int =
    when (val value = get(name)) {
        null -> 1
        is Number -> value.toInt().also { weekday ->
            require(value.toDouble() == weekday.toDouble() && weekday in 1..7) {
                "Firestore field '$name' must be an integer from 1 (Monday) to 7 (Sunday)."
            }
        }
        else -> throw IllegalArgumentException(
            "Firestore field '$name' must be an integer from 1 (Monday) to 7 (Sunday).",
        )
    }

private fun com.google.firebase.firestore.DocumentSnapshot.stringField(name: String): String =
    when (val value = get(name)) {
        null -> ""
        is String -> value
        else -> throw IllegalArgumentException("Firestore field '$name' must be a string.")
    }

private fun com.google.firebase.firestore.DocumentSnapshot.timeField(name: String): LocalTime =
    when (val value = get(name)) {
        null -> LocalTime(0, 0)
        is String -> try {
            LocalTime.parse(value)
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException("Firestore field '$name' must be an ISO time string such as '19:00'.", exception)
        }
        else -> throw IllegalArgumentException("Firestore field '$name' must be an ISO time string such as '19:00'.")
    }

private fun CongregationMetadata.toFirestoreFields(): Map<String, Any> = mapOf(
    "name" to name,
    "midweek_meeting_time" to midweekMeetingTime.toString(),
    "midweek_meeting_weekday" to midweekMeetingWeekday,
    "weekend_meeting_time" to weekendMeetingTime.toString(),
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

private const val FIRESTORE_LOG_TAG = "FirebaseFirestore"
