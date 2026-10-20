package de.geosphere.congregationplaner.user.presentation

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserPermission
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.usecase.ObserveMembersUseCase
import de.geosphere.congregationplaner.user.domain.usecase.RemoveMemberUseCase
import de.geosphere.congregationplaner.user.domain.usecase.UpdateMemberUseCase
import de.geosphere.congregationplaner.user.testing.FakeUserRepository
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserManagementViewModelTest {
    private val admin = AppUser(
        uid = "admin",
        displayName = "Admin",
        approved = true,
        role = UserRole.CONGREGATION_ADMIN,
        congregation = testCongregation,
    )
    private val pending = AppUser(
        uid = "neu",
        email = "neu@example.com",
        permissions = UserPermission.defaults,
        congregation = testCongregation,
    )

    private lateinit var repository: FakeUserRepository
    private lateinit var viewModel: UserManagementViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeUserRepository(members = listOf(admin, pending))
        viewModel = UserManagementViewModel(
            ObserveMembersUseCase(repository),
            UpdateMemberUseCase(repository),
            RemoveMemberUseCase(repository),
            UserPermissionPolicy(),
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `congregation admin sees members and invite link of the own congregation`() {
        viewModel.onCurrentUserChanged(admin)

        val state = viewModel.uiState.value
        assertTrue(state.canManage)
        assertFalse(state.isLoading)
        assertEquals(listOf("neu", "admin"), state.members.map { it.uid })
        assertEquals(testCongregation.toInviteLink(), state.inviteLink)
        assertEquals(testCongregation, repository.requestedCongregation)
        assertTrue(viewModel.canEdit(pending))
        assertFalse(viewModel.canEdit(admin))
    }

    @Test
    fun `members without admin role see nothing`() {
        viewModel.onCurrentUserChanged(pending.copy(approved = true))

        val state = viewModel.uiState.value
        assertFalse(state.canManage)
        assertTrue(state.members.isEmpty())
        assertNull(state.inviteLink)
        assertNull(repository.requestedCongregation)
    }

    @Test
    fun `admin approves members and changes roles and permissions`() {
        viewModel.onCurrentUserChanged(admin)

        viewModel.setApproved(pending, true)
        viewModel.setRole(pending, UserRole.CONGREGATION_ADMIN)
        viewModel.togglePermission(pending, UserPermission.DUMMY)
        viewModel.togglePermission(pending.copy(permissions = emptySet()), UserPermission.DUMMY)

        assertEquals(
            listOf(
                pending.copy(approved = true),
                pending.copy(role = UserRole.CONGREGATION_ADMIN),
                pending.copy(permissions = emptySet()),
                pending.copy(permissions = setOf(UserPermission.DUMMY)),
            ),
            repository.updatedMembers,
        )
        assertEquals(pending.copy(permissions = setOf(UserPermission.DUMMY)), viewModel.uiState.value.members.first())
        assertTrue(viewModel.uiState.value.busyMemberIds.isEmpty())
    }

    @Test
    fun `admin cannot modify the own membership`() {
        viewModel.onCurrentUserChanged(admin)

        viewModel.setRole(admin, UserRole.MEMBER)
        viewModel.removeMember(admin)

        assertTrue(repository.updatedMembers.isEmpty())
        assertTrue(repository.removedUids.isEmpty())
        assertEquals("Dafür fehlt dir die Berechtigung.", viewModel.uiState.value.errorMessage)

        viewModel.dismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `removing a member drops it from the list`() {
        viewModel.onCurrentUserChanged(admin)

        viewModel.removeMember(pending)

        assertEquals(listOf("neu"), repository.removedUids)
        assertEquals(listOf("admin"), viewModel.uiState.value.members.map { it.uid })
    }

    @Test
    fun `failures are reported and release the member`() {
        viewModel.onCurrentUserChanged(admin)
        repository.updateResult = Result.failure(IllegalStateException("permission-denied"))
        repository.removeResult = Result.failure(IllegalStateException("offline"))

        viewModel.setApproved(pending, true)
        assertEquals("permission-denied", viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.busyMemberIds.isEmpty())

        viewModel.removeMember(pending)
        assertEquals("offline", viewModel.uiState.value.errorMessage)
        assertEquals(2, viewModel.uiState.value.members.size)
    }

    @Test
    fun `losing the admin role stops managing members`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.onCurrentUserChanged(admin.copy(role = UserRole.MEMBER))

        val state = viewModel.uiState.value
        assertFalse(state.canManage)
        assertTrue(state.members.isEmpty())
        viewModel.setApproved(pending, true)
        assertTrue(repository.updatedMembers.isEmpty())
    }
}
