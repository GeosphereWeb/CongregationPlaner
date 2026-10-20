package de.geosphere.congregationplaner.user.presentation

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserAccessState
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.usecase.ObserveUserAccessUseCase
import de.geosphere.congregationplaner.user.testing.FakeUserRepository
import de.geosphere.congregationplaner.user.testing.testAuthUser
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserAccessViewModelTest {
    private val authUser = testAuthUser()
    private val pending = AppUser(uid = "uid-1", approved = false)

    private lateinit var repository: FakeUserRepository
    private lateinit var viewModel: UserAccessViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeUserRepository(pending)
        viewModel = UserAccessViewModel(ObserveUserAccessUseCase(repository), UserPermissionPolicy())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `state is loading until the session is known`() {
        assertEquals(UserAccessState.Loading, viewModel.accessState.value)
    }

    @Test
    fun `signed out session is unauthenticated`() {
        viewModel.onSignedOut()

        assertEquals(UserAccessState.Unauthenticated, viewModel.accessState.value)
    }

    @Test
    fun `signed in user without congregation must select one`() {
        viewModel.onSignedIn(authUser, null)

        assertEquals(UserAccessState.CongregationRequired, viewModel.accessState.value)
        assertEquals(0, repository.loadCount)
    }

    @Test
    fun `signed in user follows approval changes in the selected congregation`() {
        viewModel.onSignedIn(authUser, testCongregation)
        assertEquals(UserAccessState.NeedsApproval(pending), viewModel.accessState.value)
        assertEquals(testCongregation, repository.requestedCongregation)

        val approved = pending.copy(approved = true, role = UserRole.CONGREGATION_ADMIN)
        repository.user.value = approved
        assertEquals(UserAccessState.Authorized(approved), viewModel.accessState.value)
    }

    @Test
    fun `refresh reloads the user only while signed in`() {
        viewModel.refresh()
        assertEquals(0, repository.loadCount)

        viewModel.onSignedIn(authUser, testCongregation)
        viewModel.refresh()
        assertEquals(2, repository.loadCount)

        viewModel.onSignedOut()
        viewModel.refresh()
        assertEquals(2, repository.loadCount)
    }

    @Test
    fun `user management permission is delegated to policy`() {
        assertTrue(viewModel.canManageUsers(AppUser(approved = true, role = UserRole.CONGREGATION_ADMIN)))
        assertFalse(viewModel.canManageUsers(AppUser(approved = true, role = UserRole.MEMBER)))
    }
}
