package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.testing.FakeCongregationMetadataRepository
import de.geosphere.congregationplaner.talk.domain.model.Chairman
import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.Speaker
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.ChangeYearSlotTimesUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ConfirmSpeakerUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.CreateYearSlotsUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteTalkSlotUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.MoveTalkSlotUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveExternalDirectoryUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveExternalSpeakersUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkCatalogUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkMembersUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkYearUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveWeekEventsUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveTalkSlotUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SyncTalkHistoryWithSlotUseCase
import de.geosphere.congregationplaner.talk.testing.FakeExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.testing.FakeTalkCatalogRepository
import de.geosphere.congregationplaner.talk.testing.FakeTalkHistoryRepository
import de.geosphere.congregationplaner.talk.testing.FakeTalkScheduleRepository
import de.geosphere.congregationplaner.talk.testing.FakeWeekEventRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.Appointment
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.model.MemberCategory
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.usecase.ObserveMembersUseCase
import de.geosphere.congregationplaner.user.testing.FakeUserRepository
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TalkScheduleViewModelTest {
    private val planner = AppUser(
        uid = "planner",
        approved = true,
        roles = setOf(CongregationRole.TALK_PLANNER),
        congregation = testCongregation,
    )
    private val viewer = planner.copy(
        uid = "viewer",
        roles = setOf(CongregationRole.TALK_PLANNER, CongregationRole.TASK_PLANNER),
    )
    private val member = AppUser(uid = "member", approved = true, congregation = testCongregation)
    private val admin = AppUser(
        uid = "admin",
        approved = true,
        roles = setOf(CongregationRole.CONGREGATION_ADMIN),
        congregation = testCongregation,
    )

    private val october25 = TalkSlot(LocalDate(2026, 10, 25), LocalTime(17, 30))
    private val november1 = TalkSlot(LocalDate(2026, 11, 1), LocalTime(17, 30))
    private val kongress = WeekEvent("kongress", "Kongress", cancelsTalk = true)

    private val speakerMember = AppUser(
        uid = "m1",
        approved = true,
        profile = MemberProfile(
            lastName = "Muster",
            firstName = "Max",
            category = MemberCategory.BROTHER,
            isSpeaker = true,
        ),
    )
    private val otherMember = AppUser(
        uid = "m2",
        approved = true,
        profile = MemberProfile(lastName = "Beispiel", firstName = "Tim"),
    )
    private val nameless = AppUser(uid = "m3", approved = true, profile = MemberProfile(isSpeaker = true))
    private val elder = AppUser(
        uid = "m4",
        approved = true,
        profile = MemberProfile(
            lastName = "Alt",
            firstName = "Anton",
            category = MemberCategory.BROTHER,
            baptized = true,
            appointment = Appointment.ELDER,
        ),
    )
    private val antonAlt = Chairman("m4", "Alt", "Anton")
    private val externalSpeaker = ExternalSpeaker("e1", "Extern", "Eva", "Nord")
    private val maxMuster = Speaker("m1", "Muster", "Max")

    private val activeTalk = Talk(12, "Der Weg zum Glück")
    private val pausedTalk = Talk(40, "Pausierter Vortrag", active = false)

    private lateinit var repository: FakeTalkScheduleRepository
    private lateinit var weekEvents: FakeWeekEventRepository
    private lateinit var catalog: FakeTalkCatalogRepository
    private lateinit var users: FakeUserRepository
    private lateinit var externals: FakeExternalSpeakerDirectoryRepository
    private lateinit var history: FakeTalkHistoryRepository
    private lateinit var congregations: FakeCongregationMetadataRepository
    private lateinit var viewModel: TalkScheduleViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeTalkScheduleRepository(listOf(october25, november1))
        weekEvents = FakeWeekEventRepository(listOf(kongress))
        catalog = FakeTalkCatalogRepository(listOf(activeTalk, pausedTalk))
        users = FakeUserRepository(members = listOf(speakerMember, otherMember, nameless, elder))
        externals = FakeExternalSpeakerDirectoryRepository(
            listOf(ExternalCircuit("c1", "Kreis Nord")),
            listOf(ExternalCongregation("g1", "c1", "Nord")),
            listOf(VisitingSpeaker("e1", "g1", "Extern", "Eva")),
        )
        history = FakeTalkHistoryRepository()
        congregations = FakeCongregationMetadataRepository(
            listOf(CongregationMetadata(id = testCongregation.congregationId)),
        )
        val policy = TalkSchedulePermissionPolicy()
        val save = SaveTalkSlotUseCase(repository, policy)
        val delete = DeleteTalkSlotUseCase(repository, policy)
        viewModel = TalkScheduleViewModel(
            ObserveTalkYearUseCase(repository),
            ObserveWeekEventsUseCase(weekEvents),
            ObserveTalkCatalogUseCase(catalog),
            ObserveTalkMembersUseCase(ObserveMembersUseCase(users)),
            ObserveExternalSpeakersUseCase(ObserveExternalDirectoryUseCase(externals)),
            MoveTalkSlotUseCase(save, delete),
            CreateYearSlotsUseCase(repository, congregations, policy),
            ChangeYearSlotTimesUseCase(repository, policy),
            delete,
            SyncTalkHistoryWithSlotUseCase(history, policy),
            ConfirmSpeakerUseCase(repository, policy),
            policy,
            UserPermissionPolicy(),
        )
        viewModel.changeYear(2026)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = viewModel.uiState.value

    @Test
    fun `create year asks first and creates the missing slots after the confirmation`() {
        viewModel.onCurrentUserChanged(planner)

        viewModel.askCreateYear()

        assertTrue(state.confirmCreateYear)
        assertTrue(repository.createdSlots.isEmpty())

        viewModel.createYear()

        assertFalse(state.confirmCreateYear)
        assertFalse(state.isSaving)
        // Die beiden vorhandenen Termine liegen an Sonntagen und bleiben unverändert.
        assertEquals(50, repository.createdSlots.size)
        assertEquals("50 Termine für 2026 angelegt, 2 waren schon vorhanden.", state.infoMessage)
        assertEquals(october25, state.slots.single { it.date == october25.date })
        assertEquals(november1, state.slots.single { it.date == november1.date })
        assertEquals(52, state.slots.size)
    }

    @Test
    fun `dismissing the question or the info message changes nothing else`() {
        viewModel.onCurrentUserChanged(planner)
        viewModel.askCreateYear()

        viewModel.dismissCreateYear()

        assertFalse(state.confirmCreateYear)
        assertTrue(repository.createdSlots.isEmpty())

        viewModel.askCreateYear()
        viewModel.createYear()
        viewModel.dismissInfo()
        assertNull(state.infoMessage)
    }

    @Test
    fun `create year shows the error and keeps the existing slots`() {
        viewModel.onCurrentUserChanged(planner)
        repository.createMissingResult = Result.failure(IllegalStateException("denied"))

        viewModel.askCreateYear()
        viewModel.createYear()

        assertEquals("denied", state.errorMessage)
        assertNull(state.infoMessage)
        assertFalse(state.isSaving)
        assertEquals(listOf(october25, november1), state.slots)
    }

    @Test
    fun `create year is not possible without the permission to plan`() {
        viewModel.onCurrentUserChanged(member)

        viewModel.askCreateYear()
        viewModel.createYear()

        assertFalse(state.confirmCreateYear)
        assertTrue(repository.createdSlots.isEmpty())
        assertEquals("Dafür fehlt dir die Berechtigung.", state.errorMessage)
    }

    @Test
    fun `changing the year time asks first and then sets the time on all slots`() {
        viewModel.onCurrentUserChanged(planner)

        viewModel.askChangeYearTime()

        // The dialog starts with the most common time of the year.
        assertEquals(LocalTime(17, 30), state.yearTimeDialog)
        assertTrue(repository.savedSlots.isEmpty())

        viewModel.selectYearTime(LocalTime(9, 30))
        viewModel.changeYearTime()

        assertNull(state.yearTimeDialog)
        assertFalse(state.isSaving)
        assertEquals(listOf(october25, november1).map { it.copy(time = LocalTime(9, 30)) }, repository.savedSlots)
        assertEquals("Uhrzeit von 2 Terminen in 2026 auf 09:30 Uhr geändert.", state.infoMessage)
    }

    @Test
    fun `dismissing the year time dialog changes nothing`() {
        viewModel.onCurrentUserChanged(planner)
        viewModel.askChangeYearTime()

        viewModel.dismissYearTime()
        viewModel.changeYearTime()

        assertNull(state.yearTimeDialog)
        assertTrue(repository.savedSlots.isEmpty())
    }

    @Test
    fun `changing the year time shows the error`() {
        viewModel.onCurrentUserChanged(planner)
        repository.saveResult = Result.failure(IllegalStateException("denied"))

        viewModel.askChangeYearTime()
        viewModel.selectYearTime(LocalTime(9, 30))
        viewModel.changeYearTime()

        assertEquals("denied", state.errorMessage)
        assertNull(state.infoMessage)
        assertFalse(state.isSaving)
    }

    @Test
    fun `changing the year time is not possible without the permission to plan`() {
        viewModel.onCurrentUserChanged(member)

        viewModel.askChangeYearTime()

        assertNull(state.yearTimeDialog)
        assertEquals("Dafür fehlt dir die Berechtigung.", state.errorMessage)
    }

    @Test
    fun `status filter adds and removes statuses and survives a year change`() {
        viewModel.onCurrentUserChanged(planner)

        viewModel.toggleStatusFilter(SlotStatus.NOT_PLANNED)
        viewModel.toggleStatusFilter(SlotStatus.CANCELLED)
        assertEquals(setOf(SlotStatus.NOT_PLANNED, SlotStatus.CANCELLED), state.statusFilter)

        viewModel.toggleStatusFilter(SlotStatus.NOT_PLANNED)
        assertEquals(setOf(SlotStatus.CANCELLED), state.statusFilter)

        viewModel.changeYear(2027)
        assertEquals(setOf(SlotStatus.CANCELLED), state.statusFilter)
    }

    @Test
    fun `planner sees the slots of the year and the week events`() {
        viewModel.onCurrentUserChanged(planner)

        assertTrue(state.canPlan)
        assertFalse(state.isLoading)
        assertEquals(listOf(october25, november1), state.slots)
        assertEquals(listOf(kongress), state.weekEvents)
        assertEquals(2026, repository.requestedYear)
    }

    @Test
    fun `planner loads the talk catalog and selects an active talk as snapshot`() {
        viewModel.onCurrentUserChanged(planner)
        assertEquals(listOf(activeTalk, pausedTalk), state.talks)

        viewModel.newSlot()
        viewModel.selectTalk(activeTalk)
        assertEquals(12, state.editor!!.slot.talkNumber)
        assertEquals("Der Weg zum Glück", state.editor!!.slot.talkTitle)

        viewModel.selectTalk(null)
        assertNull(state.editor!!.slot.talkNumber)
        assertNull(state.editor!!.slot.talkTitle)
    }

    @Test
    fun `talk planner sees the speakers and selects one as snapshot`() {
        viewModel.onCurrentUserChanged(viewer)

        assertTrue(state.canViewMembers)
        assertEquals(listOf(maxMuster), state.speakers)

        viewModel.newSlot()
        viewModel.selectSpeaker(maxMuster)
        assertEquals("m1", state.editor!!.slot.speakerMemberId)
        assertEquals("Muster", state.editor!!.slot.speakerLastName)
        assertEquals("Max", state.editor!!.slot.speakerFirstName)

        viewModel.selectSpeaker(null)
        assertNull(state.editor!!.slot.speakerMemberId)
        assertNull(state.editor!!.slot.speakerLastName)
        assertNull(state.editor!!.slot.speakerFirstName)
    }

    @Test
    fun `talk planner sees the chairmen and selects one as snapshot`() {
        viewModel.onCurrentUserChanged(viewer)

        assertEquals(listOf(antonAlt), state.chairmen)

        viewModel.newSlot()
        viewModel.selectChairman(antonAlt)
        assertEquals("m4", state.editor!!.slot.chairmanMemberId)
        assertEquals("Alt", state.editor!!.slot.chairmanLastName)
        assertEquals("Anton", state.editor!!.slot.chairmanFirstName)
        assertEquals(antonAlt, state.editor!!.slot.chairman)

        viewModel.selectChairman(null)
        assertNull(state.editor!!.slot.chairmanMemberId)
        assertNull(state.editor!!.slot.chairmanLastName)
        assertNull(state.editor!!.slot.chairmanFirstName)
    }

    @Test
    fun `speaker and chairman cannot be the same member`() {
        viewModel.onCurrentUserChanged(viewer)
        viewModel.newSlot()
        viewModel.selectChairman(antonAlt)

        viewModel.selectSpeaker(Speaker("m4", "Alt", "Anton"))

        assertNull(state.editor!!.slot.speakerMemberId)
        assertEquals(antonAlt, state.editor!!.slot.chairman)
        assertEquals("Redner und Vorsitzender dürfen nicht dieselbe Person sein.", state.errorMessage)

        viewModel.dismissError()
        viewModel.selectChairman(null)
        viewModel.selectSpeaker(Speaker("m4", "Alt", "Anton"))
        assertEquals("m4", state.editor!!.slot.speakerMemberId)

        viewModel.selectChairman(antonAlt)
        assertNull(state.editor!!.slot.chairmanMemberId)
        assertNotNull(state.errorMessage)

        viewModel.dismissError()
        viewModel.selectExternalSpeaker(externalSpeaker)
        viewModel.selectChairman(antonAlt)
        assertEquals(antonAlt, state.editor!!.slot.chairman)
    }

    @Test
    fun `chairman can still be selected when the talk is cancelled`() {
        viewModel.onCurrentUserChanged(viewer)
        viewModel.newSlot()
        viewModel.updateEditor(state.editor!!.slot.copy(talkCancelled = true))

        viewModel.selectChairman(antonAlt)

        assertEquals(antonAlt, state.editor!!.slot.chairman)
    }

    @Test
    fun `external speakers load for planners`() {
        viewModel.onCurrentUserChanged(planner)

        assertEquals(listOf(externalSpeaker), state.externalSpeakers)

        viewModel.onCurrentUserChanged(member)
        assertTrue(state.externalSpeakers.isEmpty())
    }

    @Test
    fun `an external speaker replaces a member as speaker and vice versa`() {
        viewModel.onCurrentUserChanged(viewer)
        viewModel.newSlot()
        viewModel.selectSpeaker(maxMuster)

        viewModel.selectExternalSpeaker(externalSpeaker)
        val external = state.editor!!.slot
        assertNull(external.speakerMemberId)
        assertEquals(externalSpeaker, external.externalSpeaker)
        assertEquals("Nord", external.speakerCongregation)

        viewModel.selectSpeaker(maxMuster)
        val member = state.editor!!.slot
        assertNull(member.externalSpeakerId)
        assertNull(member.speakerCongregation)
        assertEquals(maxMuster, member.speaker)

        viewModel.selectExternalSpeaker(null)
        assertNull(state.editor!!.slot.speaker)
        assertNull(state.editor!!.slot.externalSpeaker)
    }

    @Test
    fun `talk planner sees the speakers of the members`() {
        viewModel.onCurrentUserChanged(planner)

        assertTrue(state.canPlan)
        assertTrue(state.canViewMembers)
        assertEquals(listOf(maxMuster), state.speakers)
        assertEquals(testCongregation, users.requestedCongregation)
    }

    @Test
    fun `losing the planner role clears the speakers`() {
        viewModel.onCurrentUserChanged(viewer)
        assertEquals(listOf(maxMuster), state.speakers)

        viewModel.onCurrentUserChanged(member)

        assertTrue(state.speakers.isEmpty())
        assertTrue(state.chairmen.isEmpty())
        assertFalse(state.canViewMembers)
    }

    @Test
    fun `an inactive talk cannot be selected`() {
        viewModel.onCurrentUserChanged(planner)
        viewModel.newSlot()

        viewModel.selectTalk(pausedTalk)

        assertNull(state.editor!!.slot.talkNumber)
    }

    @Test
    fun `talk helper sees the slots and speakers and cannot create a slot`() {
        val helper = planner.copy(uid = "helper", roles = setOf(CongregationRole.TALK_HELPER))
        repository.slots.value = listOf(october25)

        viewModel.onCurrentUserChanged(helper)
        viewModel.newSlot()

        assertTrue(state.canView)
        assertFalse(state.canPlan)
        assertEquals(listOf(october25), state.slots)
        assertTrue(state.canViewMembers)
        assertTrue(state.speakers.isNotEmpty())
        assertTrue(state.talks.isEmpty())
        assertTrue(state.weekEvents.isEmpty())
        assertNull(state.editor)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun `switching from planner to helper closes a new slot but keeps a saved one`() {
        val helper = planner.copy(uid = "helper", roles = setOf(CongregationRole.TALK_HELPER))
        viewModel.onCurrentUserChanged(planner)
        viewModel.newSlot()
        assertNotNull(state.editor)

        viewModel.onCurrentUserChanged(helper)
        assertNull(state.editor)

        viewModel.editSlot(october25)
        assertNotNull(state.editor)
        viewModel.onCurrentUserChanged(planner)
        viewModel.onCurrentUserChanged(helper)
        assertNotNull(state.editor)
        assertTrue(state.canView)
    }

    private val plannedSlot = october25.copy(
        talkNumber = 12,
        talkTitle = "Titel",
        speakerMemberId = "m1",
        speakerLastName = "Muster",
        speakerFirstName = "Max",
    )

    @Test
    fun `talk helper marks the speaker as informed and saves only that`() {
        val helper = planner.copy(uid = "helper", roles = setOf(CongregationRole.TALK_HELPER))
        repository.slots.value = listOf(plannedSlot)
        viewModel.onCurrentUserChanged(helper)

        viewModel.editSlot(plannedSlot)
        viewModel.setSpeakerInformed(true)
        assertTrue(state.editor!!.isDirty)
        viewModel.save()

        assertEquals(listOf(plannedSlot.date to true), repository.confirmedUpdates)
        assertTrue(repository.savedSlots.isEmpty())
        assertFalse(state.editor!!.isDirty)
        assertTrue(state.editor!!.slot.confirmed)
        assertFalse(state.isSaving)
        assertNull(state.errorMessage)
    }

    @Test
    fun `talk helper cannot mark an incomplete slot as informed`() {
        val helper = planner.copy(uid = "helper", roles = setOf(CongregationRole.TALK_HELPER))
        repository.slots.value = listOf(october25)
        viewModel.onCurrentUserChanged(helper)

        viewModel.editSlot(october25)
        viewModel.setSpeakerInformed(true)

        assertFalse(state.editor!!.slot.confirmed)
    }

    @Test
    fun `a failing informed save keeps the editor and shows the error`() {
        val helper = planner.copy(uid = "helper", roles = setOf(CongregationRole.TALK_HELPER))
        repository.slots.value = listOf(plannedSlot)
        repository.confirmResult = Result.failure(IllegalStateException("offline"))
        viewModel.onCurrentUserChanged(helper)

        viewModel.editSlot(plannedSlot)
        viewModel.setSpeakerInformed(true)
        viewModel.save()

        assertEquals("offline", state.errorMessage)
        assertTrue(state.editor!!.isDirty)
        assertFalse(state.isSaving)
    }

    @Test
    fun `discarding drops the informed change of the helper`() {
        val helper = planner.copy(uid = "helper", roles = setOf(CongregationRole.TALK_HELPER))
        repository.slots.value = listOf(plannedSlot)
        viewModel.onCurrentUserChanged(helper)

        viewModel.editSlot(plannedSlot)
        viewModel.setSpeakerInformed(true)
        viewModel.discardChanges()

        assertFalse(state.editor!!.slot.confirmed)
    }

    @Test
    fun `a member without roles cannot open an editor or mark informed`() {
        viewModel.onCurrentUserChanged(member)

        viewModel.editSlot(plannedSlot)
        viewModel.setSpeakerInformed(true)
        viewModel.save()

        assertNull(state.editor)
        assertTrue(repository.confirmedUpdates.isEmpty())
    }

    @Test
    fun `member without permission does not load the catalog`() {
        viewModel.onCurrentUserChanged(member)

        assertTrue(state.talks.isEmpty())
    }

    @Test
    fun `member without permission neither loads nor edits`() {
        viewModel.onCurrentUserChanged(member)
        viewModel.newSlot()

        assertFalse(state.canPlan)
        assertTrue(state.slots.isEmpty())
        assertNull(repository.requestedYear)
        assertNull(state.editor)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun `congregation admin may not plan without the talk planner role`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.newSlot()

        assertFalse(state.canPlan)
        assertNull(state.editor)
    }

    @Test
    fun `congregation admin with the talk planner role may plan`() {
        viewModel.onCurrentUserChanged(admin.copy(roles = setOf(CongregationRole.TALK_PLANNER)))
        viewModel.newSlot()

        assertTrue(state.canPlan)
        assertNotNull(state.editor)
    }

    @Test
    fun `new slot starts in the selected year and week event presets cancelled flag`() {
        viewModel.onCurrentUserChanged(planner)
        viewModel.newSlot()
        assertTrue(state.editor!!.isNew)
        assertEquals(2026, state.editor!!.slot.date.year)

        viewModel.selectWeekEvent(kongress)
        assertEquals("kongress", state.editor!!.slot.weekEventId)
        assertEquals("Kongress", state.editor!!.slot.weekEventName)
        assertTrue(state.editor!!.slot.talkCancelled)

        viewModel.selectWeekEvent(null)
        assertNull(state.editor!!.slot.weekEventId)
        assertNull(state.editor!!.slot.weekEventName)
    }

    @Test
    fun `saving a new slot stores it and keeps the editor open`() {
        viewModel.onCurrentUserChanged(planner)
        viewModel.newSlot()
        val slot = TalkSlot(LocalDate(2026, 11, 8), LocalTime(10, 0))
        viewModel.updateEditor(slot)

        viewModel.save()

        assertEquals(listOf(slot), repository.savedSlots)
        assertEquals(slot, state.editor?.original)
        assertFalse(state.isSaving)
    }

    @Test
    fun `saving a slot with a talk records the day in the talk history and deleting it removes it`() {
        viewModel.onCurrentUserChanged(planner)
        viewModel.newSlot()
        val slot = TalkSlot(LocalDate(2026, 11, 8), LocalTime(10, 0), talkNumber = 12, talkTitle = "Der Weg zum Glück")
        viewModel.updateEditor(slot)

        viewModel.save()

        assertEquals(listOf(TalkHistoryEntry(12, listOf(slot.date), listOf(slot.date))), history.entries.value)

        viewModel.delete(slot)

        assertTrue(history.entries.value.isEmpty())
    }

    @Test
    fun `a history failure keeps the saved slot and reports the history error`() {
        history.saveResult = Result.failure(IllegalStateException("denied"))
        viewModel.onCurrentUserChanged(planner)
        viewModel.newSlot()
        val slot = TalkSlot(LocalDate(2026, 11, 8), LocalTime(10, 0), talkNumber = 12, talkTitle = "Der Weg zum Glück")
        viewModel.updateEditor(slot)

        viewModel.save()

        assertEquals(listOf(slot), repository.savedSlots)
        assertEquals(slot, state.editor?.original)
        assertTrue(state.errorMessage!!.contains("Vortragshistorie"))
        assertFalse(state.isSaving)
    }

    @Test
    fun `saving on a day that already has a slot is refused`() {
        viewModel.onCurrentUserChanged(planner)
        viewModel.newSlot()
        viewModel.updateEditor(october25.copy(time = LocalTime(9, 0)))

        viewModel.save()

        assertTrue(repository.savedSlots.isEmpty())
        assertNotNull(state.errorMessage)
    }

    @Test
    fun `changing the date of an existing slot moves it`() {
        viewModel.onCurrentUserChanged(planner)
        viewModel.editSlot(october25)
        val moved = october25.copy(date = LocalDate(2026, 10, 24))
        viewModel.updateEditor(moved)

        viewModel.save()

        assertEquals(listOf(moved), repository.savedSlots)
        assertEquals(listOf(october25.date), repository.deletedDates)
    }

    @Test
    fun `saving in another year switches the list to that year`() {
        viewModel.onCurrentUserChanged(planner)
        viewModel.newSlot()
        viewModel.updateEditor(TalkSlot(LocalDate(2027, 1, 3), LocalTime(17, 30)))

        viewModel.save()

        assertEquals(2027, state.year)
        assertEquals(2027, repository.requestedYear)
    }

    @Test
    fun `failed save keeps the editor and shows the error`() {
        repository.saveResult = Result.failure(IllegalStateException("offline"))
        viewModel.onCurrentUserChanged(planner)
        viewModel.newSlot()
        viewModel.updateEditor(TalkSlot(LocalDate(2026, 11, 8), LocalTime(10, 0)))

        viewModel.save()

        assertEquals("offline", state.errorMessage)
        assertTrue(state.editor!!.isNew)
        assertFalse(state.isSaving)
    }

    @Test
    fun `dismissing a load error loads the year again`() {
        repository.observeError = IllegalStateException("denied")
        viewModel.onCurrentUserChanged(planner)
        assertEquals("denied", state.errorMessage)
        assertFalse(state.isLoading)

        repository.observeError = null
        viewModel.dismissError()

        assertNull(state.errorMessage)
        assertEquals(listOf(october25, november1), state.slots)
    }

    @Test
    fun `editor is locked while saving so the result cannot overwrite edits`() {
        viewModel.onCurrentUserChanged(planner)
        viewModel.newSlot()
        val slot = TalkSlot(LocalDate(2026, 11, 8), LocalTime(10, 0))
        viewModel.updateEditor(slot)
        val gate = CompletableDeferred<Unit>()
        repository.saveGate = gate

        viewModel.save()
        assertTrue(state.isSaving)
        viewModel.updateEditor(slot.copy(talkCancelled = true))
        viewModel.selectWeekEvent(kongress)
        viewModel.editSlot(october25)
        viewModel.newSlot()
        assertEquals(slot, state.editor?.slot)

        gate.complete(Unit)
        assertFalse(state.isSaving)
        assertEquals(slot, state.editor?.original)
    }

    @Test
    fun `deleting the open slot closes the editor`() {
        viewModel.onCurrentUserChanged(planner)
        viewModel.editSlot(october25)

        viewModel.delete(october25)

        assertEquals(listOf(october25.date), repository.deletedDates)
        assertNull(state.editor)
    }

    @Test
    fun `discard restores an existing slot and closes a new one`() {
        viewModel.onCurrentUserChanged(planner)
        viewModel.editSlot(october25)
        viewModel.updateEditor(october25.copy(talkCancelled = true))
        viewModel.discardChanges()
        assertEquals(october25, state.editor?.slot)

        viewModel.newSlot()
        viewModel.discardChanges()
        assertNull(state.editor)
    }
}
