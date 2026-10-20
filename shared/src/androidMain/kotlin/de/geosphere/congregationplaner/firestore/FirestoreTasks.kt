package de.geosphere.congregationplaner.firestore

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val FIRESTORE_LOG_TAG = "FirebaseFirestore"

internal fun logFirestoreFailure(operation: String, exception: Throwable) {
    Log.e(FIRESTORE_LOG_TAG, "Failed to $operation.", exception)
}

internal suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { value ->
        if (continuation.isActive) continuation.resume(value)
    }
    addOnFailureListener { exception ->
        if (continuation.isActive) continuation.resumeWithException(exception)
    }
}

/**
 * Führt [block] aus und bildet Fehler (außer Abbruch) auf [Result.failure] ab. Fehler, für die
 * [isExpected] zutrifft, sind fachliche Ergebnisse und werden nicht geloggt.
 */
internal suspend fun <T> runFirestoreCatching(
    operation: String,
    isExpected: (Exception) -> Boolean = { false },
    block: suspend () -> T,
): Result<T> = try {
    Result.success(block())
} catch (exception: CancellationException) {
    throw exception
} catch (exception: Exception) {
    if (!isExpected(exception)) logFirestoreFailure(operation, exception)
    Result.failure(exception)
}

/** Beobachtet alle Dokumente von [query] und bildet sie mit [toDomain] ab; Fehler beenden den Flow. */
internal fun <T> observeDocuments(query: Query, entity: String, toDomain: (DocumentSnapshot) -> T): Flow<List<T>> =
    callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                logFirestoreFailure("observe ${entity}s", error)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot == null) {
                val exception = IllegalStateException("Firestore returned an empty $entity snapshot.")
                logFirestoreFailure("observe ${entity}s", exception)
                close(exception)
                return@addSnapshotListener
            }
            runCatching { snapshot.documents.map(toDomain) }
                .onSuccess { trySend(it) }
                .onFailure {
                    logFirestoreFailure("parse $entity snapshot", it)
                    close(it)
                }
        }
        awaitClose { registration.remove() }
    }

internal fun DocumentSnapshot.stringField(name: String): String = when (val value = get(name)) {
    null -> ""
    is String -> value
    else -> throw IllegalArgumentException("Firestore field '$name' must be a string.")
}
