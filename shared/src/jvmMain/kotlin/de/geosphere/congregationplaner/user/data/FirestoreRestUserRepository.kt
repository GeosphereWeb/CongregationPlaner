package de.geosphere.congregationplaner.user.data

import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.FirebaseAuthManager
import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.FirestoreRestDocument
import de.geosphere.congregationplaner.firestore.booleanField
import de.geosphere.congregationplaner.firestore.encode
import de.geosphere.congregationplaner.firestore.firestoreBoolean
import de.geosphere.congregationplaner.firestore.firestoreFields
import de.geosphere.congregationplaner.firestore.firestoreNullableString
import de.geosphere.congregationplaner.firestore.optionalStringField
import de.geosphere.congregationplaner.firestore.parseDocument
import de.geosphere.congregationplaner.firestore.pollingFlow
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import java.net.http.HttpClient

internal class FirestoreRestUserRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : UserRepository {
    private val client = FirestoreRestClient(httpClient, idTokenProvider, "Sign in before accessing user data.")

    override suspend fun getOrCreateUser(authUser: FirebaseUser): Result<AppUser> = runCatchingIo {
        fetchUser(authUser.uid) ?: createUser(authUser.toNewAppUser())
    }

    override fun observeUser(uid: String): Flow<AppUser?> = pollingFlow { fetchUser(uid) }

    private fun fetchUser(uid: String): AppUser? =
        client.requestOrNull("GET", client.documentUrl(USERS_COLLECTION_PATH, uid))?.let { parseDocument(it).toUser() }

    private fun createUser(user: AppUser): AppUser {
        val url = "${client.collectionUrl(USERS_COLLECTION_PATH)}?documentId=${encode(user.uid)}"
        return parseDocument(client.request("POST", url, user.toFirestoreJson())).toUser()
    }

    private fun FirestoreRestDocument.toUser(): AppUser = AppUser(
        uid = id,
        email = fields.optionalStringField("email"),
        displayName = fields.optionalStringField("displayName"),
        approved = fields.booleanField("approved"),
        role = UserRole.fromValue(fields.optionalStringField("role")),
    )

    private fun AppUser.toFirestoreJson(): String = firestoreFields(
        JsonObject().apply {
            add("email", firestoreNullableString(email))
            add("displayName", firestoreNullableString(displayName))
            add("approved", firestoreBoolean(approved))
            add("role", firestoreNullableString(role.name))
        },
    )
}
