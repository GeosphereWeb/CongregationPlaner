package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteTalkSlotUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.MoveTalkSlotUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkYearUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveWeekEventsUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveTalkSlotUseCase
import de.geosphere.congregationplaner.talk.testing.FakeTalkScheduleRepository
import de.geosphere.congregationplaner.talk.testing.FakeWeekEventRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserPermission
import de.geosphere.congregationplaner.user.domain.model.UserRole
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
        permissions = setOf(UserPermission.PLAN_TALKS),
        congregation = testCongregation,
    )
    private val member = AppUser(uid = "member", approved = true, congregation = testCongregation)
    private val admin = AppUser(
        uid = "admin",
        approved = true,
        role = UserRole.CONGREGATION_ADMIN,
        congregation = testCongregation,
    )

    private val october25 = TalkSlot(LocalDate(2026, 10, 25), LocalTime(17, 30))
    private val november1 = TalkSlot(LocalDate(2026, 11, 1), LocalTime(17, 30))
    private val kongress = WeekEvent("kongress", "Kongress", cancelsTalk = true)

    private lateinit var repository: FakeTalkScheduleRepository
    private lateinit var weekEvents: FakeWeekEventRepository
    private lateinit var viewModel: TalkScheduleViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeTalkScheduleRepository(listOf(october25, november1))
        weekEvents = FakeWeekEventRepository(listOf(kongress))
        val policy = TalkSchedulePermissionPolicy()
        val save = SaveTalkSlotUseCase(repository, policy)
        val delete = DeleteTalkSlotUseCase(repository, policy)
        viewModel = TalkScheduleViewModel(
            ObserveTalkYearUseCase(repository),
            ObserveWeekEventsUseCase(weekEvents),
            MoveTalkSlotUseCase(save, delete),
            delete,
            policy,
        )
        viewModel.changeYear(2026)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = viewModel.uiState.value

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
    fun `congregation admin may plan without PLAN_TALKS`() {
        viewModel.onCurrentUserChanged(admin)
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
