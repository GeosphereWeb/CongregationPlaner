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
import de.geosphere.congregationplaner.firestore.firestoreString
import de.geosphere.congregationplaner.firestore.firestoreStringArray
import de.geosphere.congregationplaner.firestore.optionalStringField
import de.geosphere.congregationplaner.firestore.parseDocument
import de.geosphere.congregationplaner.firestore.parseDocuments
import de.geosphere.congregationplaner.firestore.pollingFlow
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.firestore.stringArrayField
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserPermission
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import java.net.http.HttpClient

internal class FirestoreRestUserRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : UserRepository {
    private val client = FirestoreRestClient(httpClient, idTokenProvider, "Sign in before accessing user data.")

    override suspend fun getOrCreateUser(authUser: FirebaseUser, congregation: CongregationRef): Result<AppUser> =
        runCatchingIo {
            fetchUser(congregation, authUser.uid) ?: createUser(congregation, authUser.toNewAppUser(congregation))
        }

    override fun observeUser(congregation: CongregationRef, uid: String): Flow<AppUser?> =
        pollingFlow { fetchUser(congregation, uid) }

    override suspend fun isGlobalAdmin(uid: String): Result<Boolean> = runCatchingIo {
        client.requestOrNull("GET", client.documentUrl(ADMINS_COLLECTION_PATH, uid)) != null
    }

    override fun observeMembers(congregation: CongregationRef): Flow<List<AppUser>> = pollingFlow {
        parseDocuments(client.request("GET", "${client.collectionUrl(congregation.membersPath)}?pageSize=1000"))
            .map { it.toUser(congregation) }
    }

    override suspend fun updateMember(congregation: CongregationRef, member: AppUser): Result<AppUser> = runCatchingIo {
        // currentDocument.exists verhindert, dass ein PATCH auf ein entferntes Mitglied es neu anlegt.
        client.request(
            method = "PATCH",
            url = "${documentUrl(congregation, member.uid)}?${client.updateMask(ACCESS_FIELD_NAMES)}" +
                "&currentDocument.exists=true",
            body = firestoreFields(member.accessFields()),
        )
        member
    }

    override suspend fun removeMember(congregation: CongregationRef, uid: String): Result<Unit> = runCatchingIo {
        client.request("DELETE", documentUrl(congregation, uid))
    }

    private fun fetchUser(congregation: CongregationRef, uid: String): AppUser? =
        client.requestOrNull("GET", documentUrl(congregation, uid))?.let { parseDocument(it).toUser(congregation) }

    private fun createUser(congregation: CongregationRef, user: AppUser): AppUser {
        val url = "${client.collectionUrl(congregation.membersPath)}?documentId=${encode(user.uid)}"
        val fields = user.accessFields().apply {
            add("email", firestoreNullableString(user.email))
            add("displayName", firestoreNullableString(user.displayName))
        }
        return parseDocument(client.request("POST", url, firestoreFields(fields))).toUser(congregation)
    }

    private fun documentUrl(congregation: CongregationRef, uid: String): String =
        client.documentUrl(congregation.membersPath, uid)

    private fun FirestoreRestDocument.toUser(congregation: CongregationRef): AppUser = AppUser(
        uid = id,
        email = fields.optionalStringField("email"),
        displayName = fields.optionalStringField("displayName"),
        approved = fields.booleanField("approved"),
        role = UserRole.fromValue(fields.optionalStringField("role")),
        permissions = UserPermission.fromValues(fields.stringArrayField("permissions")),
        congregation = congregation,
    )

    private fun AppUser.accessFields(): JsonObject = JsonObject().apply {
        add("approved", firestoreBoolean(approved))
        add("role", firestoreString(role.name))
        add("permissions", firestoreStringArray(permissions.map { it.name }.sorted()))
    }

    private companion object {
        val ACCESS_FIELD_NAMES = listOf("approved", "role", "permissions")
    }
}
