package de.geosphere.congregationplaner.user.domain.usecase

import app.cash.turbine.test
import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserAccessState
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveUserAccessUseCaseTest {
    private val authUser = object : FirebaseUser {
        override val uid: String = "uid-1"
        override val email: String? = "user@example.com"
        override val displayName: String? = "User"
        override val idToken: String? = null
    }

    @Test
    fun `missing auth user results in unauthenticated`() = runBlocking {
        val useCase = ObserveUserAccessUseCase(FakeUserRepository(Result.success(AppUser())))

        useCase(null).test {
            assertEquals(UserAccessState.Unauthenticated, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `unapproved user needs approval and becomes authorized once approved`() = runBlocking {
        val pending = AppUser(uid = "uid-1", approved = false)
        val repository = FakeUserRepository(Result.success(pending))
        val useCase = ObserveUserAccessUseCase(repository)

        useCase(authUser).test {
            assertEquals(UserAccessState.Loading, awaitItem())
            assertEquals(UserAccessState.NeedsApproval(pending), awaitItem())

            val approved = pending.copy(approved = true, role = UserRole.ADMIN)
            repository.updates.subscriptionCount.first { it > 0 }
            repository.updates.emit(approved)
            assertEquals(UserAccessState.Authorized(approved), awaitItem())

            repository.updates.emit(null)
            assertEquals(UserAccessState.Error("Nutzerprofil wurde nicht gefunden."), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(authUser, repository.requestedAuthUser)
    }

    @Test
    fun `repository failure results in error state`() = runBlocking {
        val useCase = ObserveUserAccessUseCase(
            FakeUserRepository(Result.failure(IllegalStateException("Firestore offline"))),
        )

        useCase(authUser).test {
            assertEquals(UserAccessState.Loading, awaitItem())
            assertEquals(UserAccessState.Error("Firestore offline"), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observation failure results in error state`() = runBlocking {
        val repository = object : UserRepository {
            override suspend fun getOrCreateUser(authUser: FirebaseUser): Result<AppUser> =
                Result.success(AppUser(uid = authUser.uid, approved = true))

            override fun observeUser(uid: String): Flow<AppUser?> = flow {
                throw IllegalStateException("Listener lost")
            }
        }

        ObserveUserAccessUseCase(repository)(authUser).test {
            assertEquals(UserAccessState.Loading, awaitItem())
            assertEquals(UserAccessState.Authorized(AppUser(uid = "uid-1", approved = true)), awaitItem())
            assertEquals(UserAccessState.Error("Listener lost"), awaitItem())
            awaitComplete()
        }
    }

    private class FakeUserRepository(private val initial: Result<AppUser>) : UserRepository {
        val updates = MutableSharedFlow<AppUser?>()

        var requestedAuthUser: FirebaseUser? = null
            private set

        override suspend fun getOrCreateUser(authUser: FirebaseUser): Result<AppUser> {
            requestedAuthUser = authUser
            return initial
        }

        override fun observeUser(uid: String): Flow<AppUser?> = updates
    }
}
