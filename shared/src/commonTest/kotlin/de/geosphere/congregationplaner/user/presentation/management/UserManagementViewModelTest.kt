package de.geosphere.congregationplaner.user.presentation.management

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteVisitingSpeakerUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.MoveSlotsToMemberUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveExternalDirectoryUseCase
import de.geosphere.congregationplaner.talk.testing.FakeExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.testing.FakeTalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.Appointment
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.model.MemberCategory
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.usecase.AdoptVisitingSpeakersUseCase
import de.geosphere.congregationplaner.user.domain.usecase.CreateMemberWithoutAccountUseCase
import de.geosphere.congregationplaner.user.domain.usecase.FindLinkCandidatesUseCase
import de.geosphere.congregationplaner.user.domain.usecase.LinkMemberAccountUseCase
import de.geosphere.congregationplaner.user.domain.usecase.MemberTransferPolicy
import de.geosphere.congregationplaner.user.domain.usecase.ObserveMembersUseCase
import de.geosphere.congregationplaner.user.domain.usecase.RemoveMemberUseCase
import de.geosphere.congregationplaner.user.domain.usecase.SetMemberApprovalUseCase
import de.geosphere.congregationplaner.user.domain.usecase.SetMemberRoleUseCase
import de.geosphere.congregationplaner.user.domain.usecase.UpdateMemberProfileUseCase
import de.geosphere.congregationplaner.user.testing.FakeUserRepository
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
        roles = setOf(CongregationRole.CONGREGATION_ADMIN),
        congregation = testCongregation,
    )
    private val pending = AppUser(
        uid = "neu",
        email = "neu@example.com",
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

    private val policy = UserPermissionPolicy()

    private fun createViewModel() = UserManagementViewModel(
        ObserveMembersUseCase(repository),
        MemberAccessUseCases(
            SetMemberApprovalUseCase(repository, policy),
            SetMemberRoleUseCase(repository, policy),
            RemoveMemberUseCase(repository, policy),
        ),
        MemberEditorUseCases(
            CreateMemberWithoutAccountUseCase(repository, policy),
            UpdateMemberProfileUseCase(repository, policy),
        ),
        transferUseCases(),
        policy,
    )

    private val schedule = FakeTalkScheduleRepository()
    private val directory = FakeExternalSpeakerDirectoryRepository(
        congregations = listOf(ExternalCongregation("c1", "k1", "Nord")),
        speakers = listOf(VisitingSpeaker("e1", "c1", "Berg", "Lea"), VisitingSpeaker("e2", "c1", "Alt", "Anton")),
    )

    private fun transferUseCases(
        directory: ExternalSpeakerDirectoryRepository = this.directory,
    ): MemberTransferUseCases {
        val talkPolicy = TalkSchedulePermissionPolicy()
        val transferPolicy = MemberTransferPolicy(policy, talkPolicy)
        val moveSlots = MoveSlotsToMemberUseCase(schedule, talkPolicy)
        return MemberTransferUseCases(
            ObserveExternalDirectoryUseCase(directory),
            AdoptVisitingSpeakersUseCase(
                CreateMemberWithoutAccountUseCase(repository, policy),
                moveSlots,
                DeleteVisitingSpeakerUseCase(directory, talkPolicy),
                transferPolicy,
            ),
            LinkMemberAccountUseCase(
                UpdateMemberProfileUseCase(repository, policy),
                moveSlots,
                RemoveMemberUseCase(repository, policy),
                transferPolicy,
            ),
            transferPolicy,
            FindLinkCandidatesUseCase(),
        )
    }

    private val adminPlanner get() = admin.copy(roles = admin.roles + CongregationRole.TALK_PLANNER)

    @Test
    fun `only an admin with the planner role may transfer`() {
        viewModel.onCurrentUserChanged(admin)
        assertFalse(viewModel.uiState.value.canTransfer)
        viewModel.transfer.openAdopt()
        assertNull(viewModel.uiState.value.adoptDialog)

        viewModel.onCurrentUserChanged(adminPlanner)
        assertTrue(viewModel.uiState.value.canTransfer)
    }

    @Test
    fun `chosen speakers become members`() {
        viewModel.onCurrentUserChanged(adminPlanner)
        viewModel.transfer.openAdopt()
        assertEquals(
            listOf(SpeakerChoice("e2", "Alt, Anton", "Nord"), SpeakerChoice("e1", "Berg, Lea", "Nord")),
            viewModel.uiState.value.adoptDialog?.speakers,
        )

        viewModel.transfer.toggleSpeaker("e1")
        viewModel.transfer.toggleSpeaker("e2")
        viewModel.transfer.toggleSpeaker("e2")
        viewModel.transfer.adopt()

        assertNull(viewModel.uiState.value.adoptDialog)
        assertEquals("1 Redner ist jetzt Mitglied der Versammlung.", viewModel.uiState.value.infoMessage)
        assertEquals(listOf("Berg"), repository.createdMembers.map { it.profile.lastName })
        assertEquals(listOf("e1"), directory.deletedIds)
    }

    @Test
    fun `the speaker list can be filtered by congregation`() {
        viewModel.onCurrentUserChanged(adminPlanner)
        viewModel.transfer.openAdopt()
        viewModel.transfer.toggleSpeaker("e1")

        viewModel.transfer.filterCongregation("Süd")
        assertEquals("Süd", viewModel.uiState.value.adoptDialog?.congregationFilter)
        assertTrue(viewModel.uiState.value.adoptDialog?.visibleSpeakers.orEmpty().isEmpty())
        assertTrue(viewModel.uiState.value.adoptDialog?.selected.orEmpty().isEmpty())

        viewModel.transfer.filterCongregation(null)
        assertEquals(2, viewModel.uiState.value.adoptDialog?.visibleSpeakers?.size)
    }

    @Test
    fun `filtering keeps only visible speakers selected and lists each congregation once`() {
        val dialog = AdoptSpeakersDialogState(
            speakers = listOf(
                SpeakerChoice("1", "Berg, Lea", "nord"),
                SpeakerChoice("2", "Kern, Tom", "Musterstadt"),
                SpeakerChoice("3", "Alt, Anton", "Musterstadt"),
                SpeakerChoice("4", "Ohne, Ort", ""),
            ),
            selected = setOf("1", "2"),
            isLoading = false,
        )

        assertEquals(listOf("Musterstadt", "nord"), dialog.congregations)
        assertEquals(4, dialog.visibleSpeakers.size)

        val filtered = dialog.filteredBy("Musterstadt")
        assertEquals(listOf("2", "3"), filtered.visibleSpeakers.map { it.id })
        assertEquals(setOf("2"), filtered.selected)
        assertEquals(4, filtered.filteredBy(null).visibleSpeakers.size)
    }

    @Test
    fun `dialogs close and failures of adopting and loading are reported`() {
        viewModel.onCurrentUserChanged(adminPlanner)
        viewModel.transfer.openAdopt()
        viewModel.transfer.closeAdopt()
        assertNull(viewModel.uiState.value.adoptDialog)

        // A speaker without first name cannot become a member; the reason is shown.
        directory.speakers.value = listOf(VisitingSpeaker("e3", "c1", "Ohne", ""))
        viewModel.transfer.openAdopt()
        viewModel.transfer.toggleSpeaker("e3")
        viewModel.transfer.adopt()
        assertNull(viewModel.uiState.value.infoMessage)
        assertTrue(viewModel.uiState.value.errorMessage.orEmpty().startsWith("Ohne:"))
        viewModel.dismissError()

        val broken = object : ExternalSpeakerDirectoryRepository by directory {
            override fun observeSpeakers(congregation: CongregationRef): Flow<List<VisitingSpeaker>> =
                flow { throw IllegalStateException("offline") }
        }
        val failing = UserManagementViewModel(
            ObserveMembersUseCase(repository),
            MemberAccessUseCases(
                SetMemberApprovalUseCase(repository, policy),
                SetMemberRoleUseCase(repository, policy),
                RemoveMemberUseCase(repository, policy),
            ),
            MemberEditorUseCases(
                CreateMemberWithoutAccountUseCase(repository, policy),
                UpdateMemberProfileUseCase(repository, policy),
            ),
            transferUseCases(broken),
            policy,
        )
        failing.onCurrentUserChanged(adminPlanner)
        failing.transfer.openAdopt()
        assertNull(failing.uiState.value.adoptDialog)
        assertEquals("offline", failing.uiState.value.errorMessage)
    }

    @Test
    fun `linking is refused without rights or account and failures are reported`() {
        val person = AppUser(
            uid = "p1",
            hasAccount = false,
            congregation = testCongregation,
            profile = MemberProfile(lastName = "Muster", firstName = "Max"),
        )
        repository.members.value = listOf(admin, pending, person)
        viewModel.onCurrentUserChanged(admin)
        viewModel.transfer.openLink(pending)
        assertNull(viewModel.uiState.value.linkDialog)

        viewModel.onCurrentUserChanged(adminPlanner)
        viewModel.transfer.openLink(person)
        assertNull(viewModel.uiState.value.linkDialog)
        viewModel.transfer.openLink(pending)
        viewModel.transfer.closeLink()
        assertNull(viewModel.uiState.value.linkDialog)

        repository.updateProfileResult = Result.failure(IllegalStateException("denied"))
        viewModel.transfer.openLink(pending)
        viewModel.transfer.selectPerson("p1")
        viewModel.transfer.link()
        assertNull(viewModel.uiState.value.linkDialog)
        assertEquals("denied", viewModel.uiState.value.errorMessage)
        assertTrue(repository.removedUids.isEmpty())
    }

    @Test
    fun `an account is linked with a person without account`() {
        val person = AppUser(
            uid = "p1",
            hasAccount = false,
            congregation = testCongregation,
            profile = MemberProfile(lastName = "Muster", firstName = "Max"),
        )
        repository.members.value = listOf(admin, pending, person)
        viewModel.onCurrentUserChanged(adminPlanner)
        assertTrue(viewModel.uiState.value.canLink(pending))
        assertFalse(viewModel.uiState.value.canLink(person))

        viewModel.editor.edit(pending)
        viewModel.transfer.openLink(pending)
        viewModel.transfer.selectPerson("p1")
        viewModel.transfer.link()

        val state = viewModel.uiState.value
        assertNull(state.linkDialog)
        assertEquals(listOf("p1"), repository.removedUids)
        assertEquals("Muster", state.editor?.profile?.lastName)
        assertFalse(state.members.any { it.uid == "p1" })
    }

    private fun permissionsOf(member: AppUser) = viewModel.uiState.value.permissionsOf(member)

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
        assertTrue(permissionsOf(pending).canEdit)
        assertFalse(permissionsOf(admin).canEdit)
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
    fun `admin approves members and changes roles`() {
        viewModel.onCurrentUserChanged(admin)

        viewModel.access.setApproved(pending, true)
        viewModel.access.toggleRole(pending, CongregationRole.TALK_PLANNER)
        viewModel.access.toggleRole(
            pending.copy(roles = setOf(CongregationRole.TALK_PLANNER)),
            CongregationRole.TALK_PLANNER,
        )

        assertEquals(
            listOf(
                pending.copy(approved = true),
                pending.copy(roles = setOf(CongregationRole.TALK_PLANNER)),
                pending.copy(roles = emptySet()),
            ),
            repository.updatedMembers,
        )
        assertEquals(pending, viewModel.uiState.value.members.first())
        assertTrue(viewModel.uiState.value.busyMemberIds.isEmpty())
    }

    @Test
    fun `admin cannot modify the own membership`() {
        viewModel.onCurrentUserChanged(admin)

        viewModel.access.toggleRole(admin, CongregationRole.CONGREGATION_ADMIN)
        viewModel.access.remove(admin)

        assertTrue(repository.updatedMembers.isEmpty())
        assertTrue(repository.removedUids.isEmpty())
        assertEquals("Dafür fehlt dir die Berechtigung.", viewModel.uiState.value.errorMessage)

        viewModel.dismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `admin may change the own roles but keeps the admin role`() {
        viewModel.onCurrentUserChanged(admin)

        viewModel.access.toggleRole(admin, CongregationRole.TALK_PLANNER)

        val expected = admin.copy(roles = admin.roles + CongregationRole.TALK_PLANNER)
        assertEquals(listOf(expected), repository.updatedMembers)
        assertFalse(permissionsOf(admin).canEdit)
        assertFalse(CongregationRole.CONGREGATION_ADMIN in permissionsOf(admin).toggleableRoles)
        assertTrue(CongregationRole.TASK_PLANNER in permissionsOf(admin).toggleableRoles)
        assertEquals("Die eigene Admin-Rolle bleibt bestehen.", permissionsOf(admin).adminRoleLockReason)
    }

    @Test
    fun `removing a member drops it from the list`() {
        viewModel.onCurrentUserChanged(admin)

        viewModel.access.remove(pending)

        assertEquals(listOf("neu"), repository.removedUids)
        assertEquals(listOf("admin"), viewModel.uiState.value.members.map { it.uid })
    }

    @Test
    fun `removing the selected member closes its details`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.editor.edit(pending)

        viewModel.access.remove(pending)

        assertNull(viewModel.uiState.value.editor)
        assertFalse(viewModel.uiState.value.hasDetail)
    }

    @Test
    fun `removing another member keeps the selection`() {
        val other = pending.copy(uid = "andere", email = "andere@example.com")
        repository = FakeUserRepository(members = listOf(admin, pending, other))
        viewModel = createViewModel()
        viewModel.onCurrentUserChanged(admin)
        viewModel.editor.edit(other)

        viewModel.access.remove(pending)

        assertEquals(other, viewModel.uiState.value.editor?.member)
        assertTrue(viewModel.uiState.value.hasDetail)
    }

    @Test
    fun `failures are reported and release the member`() {
        viewModel.onCurrentUserChanged(admin)
        repository.updateResult = Result.failure(IllegalStateException("permission-denied"))
        repository.removeResult = Result.failure(IllegalStateException("offline"))

        viewModel.access.setApproved(pending, true)
        assertEquals("permission-denied", viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.busyMemberIds.isEmpty())

        viewModel.access.remove(pending)
        assertEquals("offline", viewModel.uiState.value.errorMessage)
        assertEquals(2, viewModel.uiState.value.members.size)
    }

    @Test
    fun `losing the admin role stops managing members`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.onCurrentUserChanged(admin.copy(roles = emptySet()))

        val state = viewModel.uiState.value
        assertFalse(state.canManage)
        assertTrue(state.members.isEmpty())
        viewModel.access.setApproved(pending, true)
        assertTrue(repository.updatedMembers.isEmpty())
    }

    @Test
    fun `admin records a member without account`() {
        viewModel.onCurrentUserChanged(admin)

        viewModel.editor.add()
        viewModel.editor.update(MemberProfile(lastName = "Muster", firstName = "Lena", category = MemberCategory.CHILD))
        viewModel.editor.save()

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
        viewModel.editor.edit(pending)

        viewModel.editor.update(
            MemberProfile(category = MemberCategory.BROTHER, baptized = true, appointment = Appointment.ELDER),
        )
        assertEquals(Appointment.ELDER, viewModel.uiState.value.editor?.profile?.appointment)

        viewModel.editor.update(viewModel.uiState.value.editor!!.profile.copy(category = MemberCategory.CHILD))
        assertEquals(
            MemberProfile(category = MemberCategory.CHILD, baptized = false, appointment = Appointment.NONE),
            viewModel.uiState.value.editor?.profile,
        )

        viewModel.editor.update(MemberProfile(category = MemberCategory.BROTHER, isSpeaker = true))
        assertTrue(viewModel.uiState.value.editor!!.profile.isSpeaker)
        viewModel.editor.update(viewModel.uiState.value.editor!!.profile.copy(category = MemberCategory.SISTER))
        assertFalse(viewModel.uiState.value.editor!!.profile.isSpeaker)

        viewModel.editor.cancel()
        assertNull(viewModel.uiState.value.editor)
        assertTrue(repository.updatedProfiles.isEmpty())
    }

    @Test
    fun `editor is dirty only after a change and discard keeps the member selected`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.editor.edit(pending)
        assertFalse(viewModel.uiState.value.editor!!.isDirty)

        viewModel.editor.update(MemberProfile(mobilePhone = "0170"))
        assertTrue(viewModel.uiState.value.editor!!.isDirty)

        viewModel.editor.discard()
        val editor = viewModel.uiState.value.editor!!
        assertEquals(pending, editor.member)
        assertEquals(pending.profile, editor.profile)
        assertFalse(editor.isDirty)
    }

    @Test
    fun `discard changes without editor does nothing`() {
        viewModel.onCurrentUserChanged(admin)

        viewModel.editor.discard()

        assertNull(viewModel.uiState.value.editor)
    }

    @Test
    fun `discard changes closes a new person`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.editor.add()

        viewModel.editor.discard()

        assertNull(viewModel.uiState.value.editor)
    }

    @Test
    fun `admin edits profiles including the own one`() {
        viewModel.onCurrentUserChanged(admin)
        val profile = MemberProfile(lastName = "Admin", firstName = "Anna", category = MemberCategory.SISTER)

        viewModel.editor.edit(admin)
        viewModel.editor.update(profile)
        viewModel.editor.save()

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

        viewModel.editor.edit(pending)
        viewModel.editor.update(MemberProfile(mobilePhone = "0170"))
        viewModel.editor.save()

        val state = viewModel.uiState.value
        assertEquals("permission-denied", state.errorMessage)
        assertEquals(MemberProfile(mobilePhone = "0170"), state.editor?.profile)
        assertFalse(state.isSaving)
        assertTrue(state.busyMemberIds.isEmpty())

        viewModel.editor.update(MemberProfile(mobilePhone = "abc"))
        viewModel.editor.save()
        assertEquals(
            "Telefonnummern dürfen nur Ziffern, Leerzeichen und + ( ) / - enthalten (max. 30 Zeichen).",
            viewModel.uiState.value.errorMessage,
        )
    }

    @Test
    fun `members without admin role cannot edit profiles`() {
        val member = pending.copy(approved = true)
        viewModel.onCurrentUserChanged(member)

        assertFalse(permissionsOf(member).canEditProfile)
        viewModel.editor.edit(member)
        viewModel.editor.add()

        assertNull(viewModel.uiState.value.editor)
        assertEquals("Dafür fehlt dir die Berechtigung.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `losing the admin role closes the editor`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.editor.add()

        viewModel.onCurrentUserChanged(admin.copy(roles = emptySet()))
        viewModel.editor.save()

        assertNull(viewModel.uiState.value.editor)
        assertTrue(repository.createdMembers.isEmpty())
    }

    @Test
    fun `members are only listed with permissions while the user may manage them`() {
        viewModel.onCurrentUserChanged(admin)
        assertEquals(setOf("admin", "neu"), viewModel.uiState.value.memberPermissions.keys)

        viewModel.onCurrentUserChanged(admin.copy(roles = emptySet()))

        assertTrue(viewModel.uiState.value.memberPermissions.isEmpty())
    }

    @Test
    fun `an admin may remove the admin role of another admin`() {
        val other = admin.copy(uid = "other")
        repository = FakeUserRepository(members = listOf(admin, other))
        viewModel = createViewModel()
        viewModel.onCurrentUserChanged(admin)

        assertTrue(CongregationRole.CONGREGATION_ADMIN in permissionsOf(other).toggleableRoles)
        viewModel.access.toggleRole(other, CongregationRole.CONGREGATION_ADMIN)

        assertEquals(listOf(other.copy(roles = emptySet())), repository.updatedMembers)
    }

    @Test
    fun `an unapproved admin does not count and can lose the role`() {
        val unapproved = admin.copy(uid = "unapproved", approved = false)
        repository = FakeUserRepository(members = listOf(admin, unapproved))
        viewModel = createViewModel()
        viewModel.onCurrentUserChanged(admin)

        assertTrue(CongregationRole.CONGREGATION_ADMIN in permissionsOf(unapproved).toggleableRoles)
        assertNull(permissionsOf(pending).adminRoleLockReason)
    }
}
