package de.geosphere.congregationplaner.user.domain.usecase

import app.cash.turbine.test
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserAccessState
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import de.geosphere.congregationplaner.user.testing.FakeUserRepository
import de.geosphere.congregationplaner.user.testing.testAuthUser
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveUserAccessUseCaseTest {
    private val authUser = testAuthUser()

    @Test
    fun `missing auth user results in unauthenticated`() = runBlocking {
        ObserveUserAccessUseCase(FakeUserRepository(AppUser()))(null, testCongregation).test {
            assertEquals(UserAccessState.Unauthenticated, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `missing congregation requires a selection`() = runBlocking {
        ObserveUserAccessUseCase(FakeUserRepository(AppUser()))(authUser, null).test {
            assertEquals(UserAccessState.Loading, awaitItem())
            assertEquals(UserAccessState.CongregationRequired, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `unapproved member needs approval and becomes authorized once approved`() = runBlocking {
        val pending = AppUser(uid = "uid-1", approved = false)
        val repository = FakeUserRepository(pending)

        ObserveUserAccessUseCase(repository)(authUser, testCongregation).test {
            assertEquals(UserAccessState.Loading, awaitItem())
            assertEquals(UserAccessState.NeedsApproval(pending), awaitItem())
            // Das Mitgliedsdokument wird anschließend beobachtet und liefert zunächst denselben Stand.
            assertEquals(UserAccessState.NeedsApproval(pending), awaitItem())

            val approved = pending.copy(approved = true)
            repository.user.value = approved
            assertEquals(UserAccessState.Authorized(approved), awaitItem())

            repository.user.value = null
            assertEquals(UserAccessState.Error("Nutzerprofil wurde nicht gefunden."), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(testCongregation, repository.requestedCongregation)
    }

    @Test
    fun `global admin is authorized without approval`() = runBlocking {
        val repository = FakeUserRepository(AppUser(uid = "uid-1", approved = false))
        repository.globalAdminResult = Result.success(true)

        ObserveUserAccessUseCase(repository)(authUser, testCongregation).test {
            assertEquals(UserAccessState.Loading, awaitItem())
            assertEquals(
                UserAccessState.Authorized(AppUser(uid = "uid-1", approved = false, isGlobalAdmin = true)),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `global admin needs no congregation and gets no member document`() = runBlocking {
        val repository = FakeUserRepository()
        repository.globalAdminResult = Result.success(true)

        ObserveUserAccessUseCase(repository)(authUser, null).test {
            assertEquals(UserAccessState.Loading, awaitItem())
            assertEquals(
                UserAccessState.Authorized(
                    AppUser(uid = "uid-1", email = "user@example.com", approved = true, isGlobalAdmin = true),
                ),
                awaitItem(),
            )
            awaitComplete()
        }
        assertEquals(0, repository.loadCount)
    }

    @Test
    fun `global admin in a congregation without member document is not made a member`() = runBlocking {
        val repository = FakeUserRepository()
        repository.globalAdminResult = Result.success(true)

        ObserveUserAccessUseCase(repository)(authUser, testCongregation).test {
            assertEquals(UserAccessState.Loading, awaitItem())
            assertEquals(
                UserAccessState.Authorized(
                    AppUser(
                        uid = "uid-1",
                        email = "user@example.com",
                        approved = true,
                        congregation = testCongregation,
                        isGlobalAdmin = true,
                    ),
                ),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(0, repository.loadCount)
    }

    @Test
    fun `repository failures result in error state`() = runBlocking {
        val adminLookupFails = FakeUserRepository(AppUser()).apply {
            globalAdminResult = Result.failure(IllegalStateException("Admin lookup failed"))
        }
        ObserveUserAccessUseCase(adminLookupFails)(authUser, testCongregation).test {
            assertEquals(UserAccessState.Loading, awaitItem())
            assertEquals(UserAccessState.Error("Admin lookup failed"), awaitItem())
            awaitComplete()
        }

        val loadFails = FakeUserRepository().apply {
            getOrCreateResult = Result.failure(IllegalStateException("Firestore offline"))
        }
        ObserveUserAccessUseCase(loadFails)(authUser, testCongregation).test {
            assertEquals(UserAccessState.Loading, awaitItem())
            assertEquals(UserAccessState.Error("Firestore offline"), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observation failure results in error state`() = runBlocking {
        val repository = object : UserRepository by FakeUserRepository(AppUser(uid = "uid-1", approved = true)) {
            override fun observeUser(congregation: CongregationRef, uid: String): Flow<AppUser?> = flow {
                throw IllegalStateException("Listener lost")
            }
        }

        ObserveUserAccessUseCase(repository)(authUser, testCongregation).test {
            assertEquals(UserAccessState.Loading, awaitItem())
            assertEquals(UserAccessState.Authorized(AppUser(uid = "uid-1", approved = true)), awaitItem())
            assertEquals(UserAccessState.Error("Listener lost"), awaitItem())
            awaitComplete()
        }
    }
}
