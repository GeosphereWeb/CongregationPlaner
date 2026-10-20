package de.geosphere.congregationplaner.user.testing

import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal val testCongregation = CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin")

internal fun testAuthUser(uid: String = "uid-1", email: String? = "user@example.com"): FirebaseUser =
    object : FirebaseUser {
        override val uid: String = uid
        override val email: String? = email
        override val displayName: String? = null
        override val idToken: String? = null
    }

/** In-Memory-[UserRepository]; Fehler lassen sich pro Operation über die `*Result`-Felder einstellen. */
internal class FakeUserRepository(user: AppUser? = null, members: List<AppUser> = emptyList()) : UserRepository {
    val user = MutableStateFlow(user)
    val members = MutableStateFlow(members)

    var getOrCreateResult: Result<AppUser>? = null
    var globalAdminResult: Result<Boolean> = Result.success(false)
    var updateResult: Result<Unit> = Result.success(Unit)
    var removeResult: Result<Unit> = Result.success(Unit)

    var loadCount = 0
        private set
    var requestedCongregation: CongregationRef? = null
        private set
    val updatedMembers = mutableListOf<AppUser>()
    val removedUids = mutableListOf<String>()

    override suspend fun getOrCreateUser(authUser: FirebaseUser, congregation: CongregationRef): Result<AppUser> {
        loadCount++
        requestedCongregation = congregation
        return getOrCreateResult ?: Result.success(checkNotNull(user.value))
    }

    override fun observeUser(congregation: CongregationRef, uid: String): Flow<AppUser?> = user

    override suspend fun isGlobalAdmin(uid: String): Result<Boolean> = globalAdminResult

    override fun observeMembers(congregation: CongregationRef): Flow<List<AppUser>> {
        requestedCongregation = congregation
        return members
    }

    override suspend fun updateMember(congregation: CongregationRef, member: AppUser): Result<AppUser> =
        updateResult.map {
            updatedMembers += member
            member
        }

    override suspend fun removeMember(congregation: CongregationRef, uid: String): Result<Unit> =
        removeResult.onSuccess { removedUids += uid }
}
