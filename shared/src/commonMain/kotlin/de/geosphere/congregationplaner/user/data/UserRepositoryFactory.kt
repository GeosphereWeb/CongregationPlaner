package de.geosphere.congregationplaner.user.data

import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserPermission
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

expect fun createUserRepository(): UserRepository

/** Globale Administratoren; das Dokument `admins/{uid}` wird manuell (z. B. in der Firebase-Konsole) angelegt. */
internal const val ADMINS_COLLECTION_PATH = "admins"

/** Neue Mitglieder starten ohne Freigabe als einfaches Mitglied mit den Standardberechtigungen. */
internal fun FirebaseUser.toNewAppUser(congregation: CongregationRef): AppUser = AppUser(
    uid = uid,
    email = email,
    displayName = displayName,
    approved = false,
    role = UserRole.MEMBER,
    permissions = UserPermission.defaults,
    congregation = congregation,
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

    override suspend fun updateMember(congregation: CongregationRef, member: AppUser): Result<AppUser> = unavailable()

    override suspend fun removeMember(congregation: CongregationRef, uid: String): Result<Unit> = unavailable()
}
