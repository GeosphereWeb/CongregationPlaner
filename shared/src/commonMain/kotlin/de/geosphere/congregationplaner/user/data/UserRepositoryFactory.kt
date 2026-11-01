package de.geosphere.congregationplaner.user.data

import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AdminContact
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.repository.AdminContactRepository
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

expect fun createUserRepository(): UserRepository

expect fun createAdminContactRepository(): AdminContactRepository

/** Globale Administratoren; das Dokument `admins/{uid}` wird manuell (z. B. in der Firebase-Konsole) angelegt. */
internal const val ADMINS_COLLECTION_PATH = "admins"

/**
 * Neue Mitglieder starten ohne Freigabe und ohne Rolle; die persönlichen Angaben pflegt anschließend der Admin.
 */
internal fun FirebaseUser.toNewAppUser(congregation: CongregationRef): AppUser = AppUser(
    uid = uid,
    email = email,
    displayName = displayName,
    approved = false,
    congregation = congregation,
)

/** Personen ohne Konto haben nie Zugang zur App. */
internal fun MemberProfile.toNewMemberWithoutAccount(uid: String, congregation: CongregationRef): AppUser = AppUser(
    uid = uid,
    approved = false,
    congregation = congregation,
    hasAccount = false,
    profile = this,
)

fun unavailableUserRepository(platform: String): UserRepository = object : UserRepository {
    private fun <T> unavailable(): Result<T> =
        Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

    override suspend fun getOrCreateUser(authUser: FirebaseUser, congregation: CongregationRef): Result<AppUser> =
        unavailable()

    override fun observeUser(congregation: CongregationRef, uid: String): Flow<AppUser?> = flow {
        throw UnsupportedOperationException("Firestore is not configured for $platform.")
    }

    override suspend fun isGlobalAdmin(uid: String): Result<Boolean> = unavailable()

    override fun observeMembers(congregation: CongregationRef): Flow<List<AppUser>> = flow {
        throw UnsupportedOperationException("Firestore is not configured for $platform.")
    }

    override suspend fun createMemberWithoutAccount(
        congregation: CongregationRef,
        profile: MemberProfile,
    ): Result<AppUser> = unavailable()

    override suspend fun updateMember(congregation: CongregationRef, member: AppUser): Result<AppUser> = unavailable()

    override suspend fun updateMemberProfile(congregation: CongregationRef, member: AppUser): Result<AppUser> =
        unavailable()

    override suspend fun removeMember(congregation: CongregationRef, uid: String): Result<Unit> = unavailable()

    override suspend fun migrateLegacyMembers(congregation: CongregationRef): Result<Int> = unavailable()
}

fun unavailableAdminContactRepository(platform: String): AdminContactRepository = object : AdminContactRepository {
    private fun <T> unavailable(): Result<T> =
        Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

    override suspend fun getAdminContacts(congregation: CongregationRef): Result<List<AdminContact>> = unavailable()

    override suspend fun putAdminContact(congregation: CongregationRef, contact: AdminContact): Result<Unit> =
        unavailable()

    override suspend fun deleteAdminContact(congregation: CongregationRef, uid: String): Result<Unit> = unavailable()
}
