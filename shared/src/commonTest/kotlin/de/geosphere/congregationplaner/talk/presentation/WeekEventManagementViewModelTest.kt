package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.permission.TalkReferenceDataPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteWeekEventUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveWeekEventsUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveWeekEventUseCase
import de.geosphere.congregationplaner.talk.testing.FakeWeekEventRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WeekEventManagementViewModelTest {
    private val admin = AppUser(uid = "admin", isGlobalAdmin = true)
    private val congregationAdmin =
        AppUser(uid = "ca", approved = true, roles = setOf(CongregationRole.CONGREGATION_ADMIN))
    private val besuch = WeekEvent("besuch", "Besuch")
    private val kongress = WeekEvent("kongress", "Kongress", cancelsTalk = true)

    private lateinit var events: FakeWeekEventRepository
    private lateinit var viewModel: WeekEventManagementViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        events = FakeWeekEventRepository(listOf(kongress, besuch))
        val policy = TalkReferenceDataPolicy()
        viewModel = WeekEventManagementViewModel(
            ObserveWeekEventsUseCase(events),
            SaveWeekEventUseCase(events, policy),
            DeleteWeekEventUseCase(events, policy),
            policy,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = viewModel.uiState.value

    @Test
    fun `global admin sees the week events sorted by name`() {
        viewModel.onCurrentUserChanged(admin)

        assertTrue(state.canManage)
        assertFalse(state.isLoading)
        assertEquals(listOf(besuch, kongress), state.events)
    }

    @Test
    fun `other users neither load nor edit`() {
        viewModel.onCurrentUserChanged(congregationAdmin)
        viewModel.newEvent()

        assertFalse(state.canManage)
        assertTrue(state.events.isEmpty())
        assertNull(state.editor)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun `a new week event is saved with its generated id`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.newEvent()
        assertTrue(state.editor!!.isNew)

        viewModel.updateEditor(state.editor!!.copy(name = " Sonderversammlung ", cancelsTalk = true))
        viewModel.save()

        val saved = WeekEvent("ev-1", "Sonderversammlung", cancelsTalk = true)
        assertEquals(listOf(saved), events.savedEvents)
        assertEquals(saved, state.editor!!.original)
        assertFalse(state.isSaving)
    }

    @Test
    fun `saving and deleting update the list before the snapshot flow delivers`() {
        events.publishesChanges = false
        viewModel.onCurrentUserChanged(admin)
        viewModel.newEvent()
        viewModel.updateEditor(state.editor!!.copy(name = "Aaa"))

        viewModel.save()
        val saved = WeekEvent("ev-1", "Aaa", cancelsTalk = false)
        assertTrue(saved in state.events)

        viewModel.delete(saved)
        assertFalse(saved in state.events)
    }

    @Test
    fun `an existing week event keeps its id when changed`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.editEvent(kongress)
        assertFalse(state.editor!!.isDirty)

        viewModel.updateEditor(state.editor!!.copy(cancelsTalk = false))
        assertTrue(state.editor!!.isDirty)
        viewModel.save()

        assertEquals(listOf(WeekEvent("kongress", "Kongress", cancelsTalk = false)), events.savedEvents)
    }

    @Test
    fun `an empty name and repository failures show a message and keep the editor`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.newEvent()

        viewModel.save()
        assertNotNull(state.errorMessage)
        assertTrue(events.savedEvents.isEmpty())

        viewModel.dismissError()
        viewModel.updateEditor(state.editor!!.copy(name = "Neu"))
        events.saveResult = Result.failure(IllegalStateException("offline"))
        viewModel.save()
        assertEquals("offline", state.errorMessage)
        assertTrue(state.editor!!.isNew)
        assertFalse(state.isSaving)
    }

    @Test
    fun `deleting the open week event closes the editor and failures are reported`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.editEvent(kongress)

        viewModel.delete(kongress)

        assertEquals(listOf("kongress"), events.deletedIds)
        assertNull(state.editor)
        assertEquals(listOf(besuch), state.events)

        events.deleteResult = Result.failure(IllegalStateException("denied"))
        viewModel.delete(besuch)
        assertEquals("denied", state.errorMessage)
    }

    @Test
    fun `a load error is retried when dismissed and losing the permission clears everything`() {
        events.observeError = IllegalStateException("denied")
        viewModel.onCurrentUserChanged(admin)
        assertEquals("denied", state.errorMessage)

        events.observeError = null
        viewModel.dismissError()
        assertEquals(listOf(besuch, kongress), state.events)

        assertFalse(state.hasDetail)
        viewModel.newEvent()
        assertTrue(state.hasDetail)
        viewModel.cancelEditing()
        assertNull(state.editor)
        assertFalse(state.hasDetail)

        viewModel.editEvent(besuch)
        viewModel.onCurrentUserChanged(congregationAdmin)
        assertTrue(state.events.isEmpty())
        assertNull(state.editor)
    }
}
