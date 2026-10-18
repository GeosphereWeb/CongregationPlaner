package de.geosphere.congregationplaner.user.data

import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

expect fun createUserRepository(): UserRepository

internal const val USERS_COLLECTION_PATH = "users"

/** Neue Nutzer starten ohne Freigabe mit der niedrigsten Planungsrolle. */
internal fun FirebaseUser.toNewAppUser(): AppUser = AppUser(
    uid = uid,
    email = email,
    displayName = displayName,
    approved = false,
    role = UserRole.SPEAKING_ASSISTANT,
)

fun unavailableUserRepository(platform: String): UserRepository = object : UserRepository {
    override suspend fun getOrCreateUser(authUser: FirebaseUser): Result<AppUser> =
        Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

    override fun observeUser(uid: String): Flow<AppUser?> = flow {
        throw UnsupportedOperationException("Firestore is not configured for $platform.")
    }
}
