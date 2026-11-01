package de.geosphere.congregationplaner.user.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import de.geosphere.congregationplaner.auth.AndroidFirestoreProvider
import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.logFirestoreFailure
import de.geosphere.congregationplaner.firestore.observeDocuments
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.data.mappers.memberRoles
import de.geosphere.congregationplaner.user.data.mappers.toStoredRoles
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.Appointment
import de.geosphere.congregationplaner.user.domain.model.MemberCategory
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.repository.AdminContactRepository
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual fun createUserRepository(): UserRepository = FirestoreUserRepository(AndroidFirestoreProvider.firestore)
    .withAdminContacts(FirestoreAdminContactRepository(AndroidFirestoreProvider.firestore))

actual fun createAdminContactRepository(): AdminContactRepository =
    FirestoreAdminContactRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreUserRepository(private val firestore: FirebaseFirestore) : UserRepository {
    override suspend fun getOrCreateUser(authUser: FirebaseUser, congregation: CongregationRef): Result<AppUser> =
        runFirestoreCatching("get or create user") {
            val document = firestore.collection(congregation.membersPath).document(authUser.uid)
            // Source.SERVER umgeht den Cache, damit Freigaben und Rollen sofort wirksam werden.
            val snapshot = document.get(Source.SERVER).awaitResult()
            if (snapshot.exists()) {
                toDomain(snapshot, congregation)
            } else {
                authUser.toNewAppUser(congregation).also { newUser ->
                    document.set(newUser.toFirestoreFields()).awaitResult()
                }
            }
        }

    override fun observeUser(congregation: CongregationRef, uid: String): Flow<AppUser?> = callbackFlow {
        val registration = firestore.collection(congregation.membersPath)
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
                runCatching { toDomain(snapshot, congregation) }
                    .onSuccess { trySend(it) }
                    .onFailure {
                        logFirestoreFailure("parse user snapshot", it)
                        close(it)
                    }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun isGlobalAdmin(uid: String): Result<Boolean> = runFirestoreCatching("read admin flag") {
        firestore.collection(ADMINS_COLLECTION_PATH).document(uid).get(Source.SERVER).awaitResult().exists()
    }

    override fun observeMembers(congregation: CongregationRef): Flow<List<AppUser>> =
        observeDocuments(firestore.collection(congregation.membersPath), "member") { toDomain(it, congregation) }

    override suspend fun createMemberWithoutAccount(
        congregation: CongregationRef,
        profile: MemberProfile,
    ): Result<AppUser> = runFirestoreCatching("create member without account") {
        // document() ohne Pfad vergibt eine zufällige ID, die nie mit einer Firebase-UID kollidiert.
        val document = firestore.collection(congregation.membersPath).document()
        profile.toNewMemberWithoutAccount(document.id, congregation).also { member ->
            document.set(member.toFirestoreFields()).awaitResult()
        }
    }

    override suspend fun updateMember(congregation: CongregationRef, member: AppUser): Result<AppUser> =
        runFirestoreCatching("update member") {
            firestore.collection(congregation.membersPath)
                .document(member.uid)
                .update(member.toAccessFields())
                .awaitResult()
            member
        }

    override suspend fun updateMemberProfile(congregation: CongregationRef, member: AppUser): Result<AppUser> =
        runFirestoreCatching("update member profile") {
            firestore.collection(congregation.membersPath)
                .document(member.uid)
                .update(member.profile.toFirestoreFields())
                .awaitResult()
            member
        }

    override suspend fun removeMember(congregation: CongregationRef, uid: String): Result<Unit> =
        runFirestoreCatching("remove member") {
            firestore.collection(congregation.membersPath).document(uid).delete().awaitResult()
        }

    override suspend fun migrateLegacyMembers(congregation: CongregationRef): Result<Int> =
        runFirestoreCatching("migrate legacy members") {
            val snapshot = firestore.collection(congregation.membersPath).get(Source.SERVER).awaitResult()
            snapshot.documents.filterNot { it.contains("roles") }.count { migrate(it, congregation) }
        }

    // Ein Fehler bei einem Dokument wird protokolliert und lässt die übrigen weiter umstellen.
    private suspend fun migrate(document: DocumentSnapshot, congregation: CongregationRef): Boolean =
        runFirestoreCatching("migrate member ${document.id}") {
            val roles = toDomain(document, congregation).roles
            document.reference.update(
                mapOf(
                    "roles" to roles.toStoredRoles(),
                    "role" to FieldValue.delete(),
                    "permissions" to FieldValue.delete(),
                ),
            ).awaitResult()
        }.isSuccess
}

private fun toDomain(document: DocumentSnapshot, congregation: CongregationRef): AppUser = AppUser(
    uid = document.id,
    email = document.getString("email"),
    displayName = document.getString("displayName"),
    approved = document.getBoolean("approved") ?: false,
    roles = memberRoles(
        storedRoles = document.takeIf { it.contains("roles") }?.stringList("roles"),
        legacyRole = document.getString("role"),
        legacyPermissions = document.stringList("permissions").orEmpty(),
    ),
    congregation = congregation,
    hasAccount = document.getBoolean("hasAccount") ?: true,
    profile = MemberProfile(
        lastName = document.getString("lastName").orEmpty(),
        firstName = document.getString("firstName").orEmpty(),
        category = MemberCategory.fromValue(document.getString("category")),
        baptized = document.getBoolean("baptized") ?: false,
        appointment = Appointment.fromValue(document.getString("appointment")),
        mobilePhone = document.getString("mobilePhone").orEmpty(),
        landlinePhone = document.getString("landlinePhone").orEmpty(),
        isSpeaker = document.getBoolean("isSpeaker") ?: false,
    ),
)

private fun DocumentSnapshot.stringList(field: String): List<String?>? = (get(field) as? List<*>)?.map { it as? String }

private fun AppUser.toFirestoreFields(): Map<String, Any?> = mapOf(
    "email" to email,
    "displayName" to displayName,
    "hasAccount" to hasAccount,
) + toRoleFields() + profile.toFirestoreFields()

private fun AppUser.toRoleFields(): Map<String, Any> = mapOf(
    "approved" to approved,
    "roles" to roles.toStoredRoles(),
)

// Beim Ändern verschwinden die veralteten Felder `role` und `permissions` von Altdokumenten.
private fun AppUser.toAccessFields(): Map<String, Any> = toRoleFields() + mapOf(
    "role" to FieldValue.delete(),
    "permissions" to FieldValue.delete(),
)

private fun MemberProfile.toFirestoreFields(): Map<String, Any?> = mapOf(
    "lastName" to lastName,
    "firstName" to firstName,
    "category" to category?.name,
    "baptized" to baptized,
    "appointment" to appointment.name,
    "mobilePhone" to mobilePhone,
    "landlinePhone" to landlinePhone,
    "isSpeaker" to isSpeaker,
)
