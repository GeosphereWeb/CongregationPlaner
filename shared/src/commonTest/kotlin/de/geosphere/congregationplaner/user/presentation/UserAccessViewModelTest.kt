package de.geosphere.congregationplaner.user.presentation

import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserAccessState
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import de.geosphere.congregationplaner.user.domain.usecase.ObserveUserAccessUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val authUser = object : FirebaseUser {
        override val uid: String = "uid-1"
        override val email: String? = "user@example.com"
        override val displayName: String? = null
        override val idToken: String? = null
    }
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
    fun `signed in user follows approval changes`() {
        viewModel.onSignedIn(authUser)
        assertEquals(UserAccessState.NeedsApproval(pending), viewModel.accessState.value)

        val approved = pending.copy(approved = true, role = UserRole.ADMIN)
        repository.user.value = approved
        assertEquals(UserAccessState.Authorized(approved), viewModel.accessState.value)
    }

    @Test
    fun `refresh reloads the user only while signed in`() {
        viewModel.refresh()
        assertEquals(0, repository.loadCount)

        viewModel.onSignedIn(authUser)
        viewModel.refresh()
        assertEquals(2, repository.loadCount)

        viewModel.onSignedOut()
        viewModel.refresh()
        assertEquals(2, repository.loadCount)
    }

    @Test
    fun `user management permission is delegated to policy`() {
        assertTrue(viewModel.canManageUsers(AppUser(approved = true, role = UserRole.ADMIN)))
        assertFalse(viewModel.canManageUsers(AppUser(approved = true, role = UserRole.SPEAKING_PLANER)))
    }

    private class FakeUserRepository(initial: AppUser) : UserRepository {
        val user = MutableStateFlow<AppUser?>(initial)
        var loadCount = 0

        override suspend fun getOrCreateUser(authUser: FirebaseUser): Result<AppUser> {
            loadCount++
            return Result.success(checkNotNull(user.value))
        }

        override fun observeUser(uid: String): Flow<AppUser?> = user
    }
}
