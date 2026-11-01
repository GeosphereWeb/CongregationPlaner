package de.geosphere.congregationplaner.user.presentation

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.data.mappers.withLegacyRoles
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.Appointment
import de.geosphere.congregationplaner.user.domain.model.MemberCategory
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.model.UserPermission
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.usecase.CreateMemberWithoutAccountUseCase
import de.geosphere.congregationplaner.user.domain.usecase.ObserveMembersUseCase
import de.geosphere.congregationplaner.user.domain.usecase.RemoveMemberUseCase
import de.geosphere.congregationplaner.user.domain.usecase.UpdateMemberProfileUseCase
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
    ).withLegacyRoles()
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
        viewModel = createViewModel()
    }

    private fun createViewModel() = UserManagementViewModel(
        ObserveMembersUseCase(repository),
        UpdateMemberUseCase(repository),
        CreateMemberWithoutAccountUseCase(repository),
        UpdateMemberProfileUseCase(repository),
        RemoveMemberUseCase(repository),
        UserPermissionPolicy(),
    )

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
        assertEquals(testCongregation, CongregationRef.parse(state.inviteLink.orEmpty()))
        assertEquals(testCongregation, repository.requestedCongregation)
        assertTrue(viewModel.canEdit(pending))
        assertFalse(viewModel.canEdit(admin))
    }

    @Test
    fun `invite link stays the same while the user changes`() {
        viewModel.onCurrentUserChanged(admin)
        val link = viewModel.uiState.value.inviteLink

        viewModel.onCurrentUserChanged(admin.copy(displayName = "Neu"))

        assertEquals(link, viewModel.uiState.value.inviteLink)
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
    fun `admin may change the own permissions`() {
        viewModel.onCurrentUserChanged(admin)

        viewModel.togglePermission(admin, UserPermission.PLAN_TALKS)

        val expected = admin.copy(permissions = admin.permissions + UserPermission.PLAN_TALKS)
        assertEquals(listOf(expected), repository.updatedMembers)
        assertTrue(viewModel.canEditPermissions(admin))
        assertFalse(viewModel.canEdit(admin))
    }

    @Test
    fun `removing a member drops it from the list`() {
        viewModel.onCurrentUserChanged(admin)

        viewModel.removeMember(pending)

        assertEquals(listOf("neu"), repository.removedUids)
        assertEquals(listOf("admin"), viewModel.uiState.value.members.map { it.uid })
    }

    @Test
    fun `removing the selected member closes its details`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.editProfile(pending)

        viewModel.removeMember(pending)

        assertNull(viewModel.uiState.value.editor)
        assertFalse(viewModel.uiState.value.hasDetail)
    }

    @Test
    fun `removing another member keeps the selection`() {
        val other = pending.copy(uid = "andere", email = "andere@example.com")
        repository = FakeUserRepository(members = listOf(admin, pending, other))
        viewModel = createViewModel()
        viewModel.onCurrentUserChanged(admin)
        viewModel.editProfile(other)

        viewModel.removeMember(pending)

        assertEquals(other, viewModel.uiState.value.editor?.member)
        assertTrue(viewModel.uiState.value.hasDetail)
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
        viewModel.onCurrentUserChanged(admin.copy(role = UserRole.MEMBER, roles = emptySet()))

        val state = viewModel.uiState.value
        assertFalse(state.canManage)
        assertTrue(state.members.isEmpty())
        viewModel.setApproved(pending, true)
        assertTrue(repository.updatedMembers.isEmpty())
    }

    @Test
    fun `admin records a member without account`() {
        viewModel.onCurrentUserChanged(admin)

        viewModel.addMemberWithoutAccount()
        viewModel.updateEditor(MemberProfile(lastName = "Muster", firstName = "Lena", category = MemberCategory.CHILD))
        viewModel.saveProfile()

        val created = repository.createdMembers.single()
        assertFalse(created.hasAccount)
        assertEquals("Lena Muster", created.label)
        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertTrue(created in state.members)
        assertEquals(created, state.editor?.member)
        assertFalse(state.editor!!.isDirty)
    }

    @Test
    fun `editor keeps the profile consistent`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.editProfile(pending)

        viewModel.updateEditor(
            MemberProfile(category = MemberCategory.BROTHER, baptized = true, appointment = Appointment.ELDER),
        )
        assertEquals(Appointment.ELDER, viewModel.uiState.value.editor?.profile?.appointment)

        viewModel.updateEditor(viewModel.uiState.value.editor!!.profile.copy(category = MemberCategory.CHILD))
        assertEquals(
            MemberProfile(category = MemberCategory.CHILD, baptized = false, appointment = Appointment.NONE),
            viewModel.uiState.value.editor?.profile,
        )

        viewModel.updateEditor(MemberProfile(category = MemberCategory.BROTHER, isSpeaker = true))
        assertTrue(viewModel.uiState.value.editor!!.profile.isSpeaker)
        viewModel.updateEditor(viewModel.uiState.value.editor!!.profile.copy(category = MemberCategory.SISTER))
        assertFalse(viewModel.uiState.value.editor!!.profile.isSpeaker)

        viewModel.cancelEditing()
        assertNull(viewModel.uiState.value.editor)
        assertTrue(repository.updatedProfiles.isEmpty())
    }

    @Test
    fun `editor is dirty only after a change and discard keeps the member selected`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.editProfile(pending)
        assertFalse(viewModel.uiState.value.editor!!.isDirty)

        viewModel.updateEditor(MemberProfile(mobilePhone = "0170"))
        assertTrue(viewModel.uiState.value.editor!!.isDirty)

        viewModel.discardChanges()
        val editor = viewModel.uiState.value.editor!!
        assertEquals(pending, editor.member)
        assertEquals(pending.profile, editor.profile)
        assertFalse(editor.isDirty)
    }

    @Test
    fun `discard changes without editor does nothing`() {
        viewModel.onCurrentUserChanged(admin)

        viewModel.discardChanges()

        assertNull(viewModel.uiState.value.editor)
    }

    @Test
    fun `discard changes closes a new person`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.addMemberWithoutAccount()

        viewModel.discardChanges()

        assertNull(viewModel.uiState.value.editor)
    }

    @Test
    fun `admin edits profiles including the own one`() {
        viewModel.onCurrentUserChanged(admin)
        val profile = MemberProfile(lastName = "Admin", firstName = "Anna", category = MemberCategory.SISTER)

        viewModel.editProfile(admin)
        viewModel.updateEditor(profile)
        viewModel.saveProfile()

        assertEquals(listOf(admin.copy(profile = profile)), repository.updatedProfiles)
        val state = viewModel.uiState.value
        assertEquals(admin.copy(profile = profile), state.members.single { it.uid == admin.uid })
        assertEquals(admin.copy(profile = profile), state.editor?.member)
        assertFalse(state.editor!!.isDirty)
        assertTrue(state.busyMemberIds.isEmpty())
    }

    @Test
    fun `profile save failures keep the editor open`() {
        viewModel.onCurrentUserChanged(admin)
        repository.updateProfileResult = Result.failure(IllegalStateException("permission-denied"))

        viewModel.editProfile(pending)
        viewModel.updateEditor(MemberProfile(mobilePhone = "0170"))
        viewModel.saveProfile()

        val state = viewModel.uiState.value
        assertEquals("permission-denied", state.errorMessage)
        assertEquals(MemberProfile(mobilePhone = "0170"), state.editor?.profile)
        assertFalse(state.isSaving)
        assertTrue(state.busyMemberIds.isEmpty())

        viewModel.updateEditor(MemberProfile(mobilePhone = "abc"))
        viewModel.saveProfile()
        assertEquals(
            "Telefonnummern dürfen nur Ziffern, Leerzeichen und + ( ) / - enthalten (max. 30 Zeichen).",
            viewModel.uiState.value.errorMessage,
        )
    }

    @Test
    fun `members without admin role cannot edit profiles`() {
        val member = pending.copy(approved = true)
        viewModel.onCurrentUserChanged(member)

        assertFalse(viewModel.canEditProfile(member))
        viewModel.editProfile(member)
        viewModel.addMemberWithoutAccount()

        assertNull(viewModel.uiState.value.editor)
        assertEquals("Dafür fehlt dir die Berechtigung.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `losing the admin role closes the editor`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.addMemberWithoutAccount()

        viewModel.onCurrentUserChanged(admin.copy(role = UserRole.MEMBER, roles = emptySet()))
        viewModel.saveProfile()

        assertNull(viewModel.uiState.value.editor)
        assertTrue(repository.createdMembers.isEmpty())
    }
}
