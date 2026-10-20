package de.geosphere.congregationplaner.user.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import de.geosphere.congregationplaner.auth.AndroidFirestoreProvider
import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.logFirestoreFailure
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual fun createUserRepository(): UserRepository = FirestoreUserRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreUserRepository(private val firestore: FirebaseFirestore) : UserRepository {
    override suspend fun getOrCreateUser(authUser: FirebaseUser): Result<AppUser> =
        runFirestoreCatching("get or create user") {
            val document = firestore.collection(USERS_COLLECTION_PATH).document(authUser.uid)
            // Source.SERVER umgeht den Cache, damit Freigaben und Rollen sofort wirksam werden.
            val snapshot = document.get(Source.SERVER).awaitResult()
            if (snapshot.exists()) {
                toDomain(snapshot)
            } else {
                authUser.toNewAppUser().also { newUser ->
                    document.set(newUser.toFirestoreFields()).awaitResult()
                }
            }
        }

    override fun observeUser(uid: String): Flow<AppUser?> = callbackFlow {
        val registration = firestore.collection(USERS_COLLECTION_PATH)
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logFirestoreFailure("observe user", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                runCatching { toDomain(snapshot) }
                    .onSuccess { trySend(it) }
                    .onFailure {
                        logFirestoreFailure("parse user snapshot", it)
                        close(it)
                    }
            }
        awaitClose { registration.remove() }
    }
}

private fun toDomain(document: DocumentSnapshot): AppUser = AppUser(
    uid = document.id,
    email = document.getString("email"),
    displayName = document.getString("displayName"),
    approved = document.getBoolean("approved") ?: false,
    role = UserRole.fromValue(document.getString("role")),
)

private fun AppUser.toFirestoreFields(): Map<String, Any?> = mapOf(
    "email" to email,
    "displayName" to displayName,
    "approved" to approved,
    "role" to role.name,
)
