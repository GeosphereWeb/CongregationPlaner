package de.geosphere.congregationplaner.event.presentation

import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.event.domain.model.SpecialEvent
import de.geosphere.congregationplaner.event.domain.permission.EventTypePolicy
import de.geosphere.congregationplaner.event.domain.permission.SpecialEventPolicy
import de.geosphere.congregationplaner.event.domain.usecase.DeleteSpecialEventUseCase
import de.geosphere.congregationplaner.event.domain.usecase.ObserveEventTypesUseCase
import de.geosphere.congregationplaner.event.domain.usecase.ObserveManagedEventsUseCase
import de.geosphere.congregationplaner.event.domain.usecase.SaveSpecialEventUseCase
import de.geosphere.congregationplaner.event.testing.FakeEventTypeRepository
import de.geosphere.congregationplaner.event.testing.FakeSpecialEventRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SpecialEventManagementViewModelTest {
    private val admin = AppUser(
        uid = "admin",
        approved = true,
        roles = setOf(CongregationRole.CONGREGATION_ADMIN),
        congregation = testCongregation,
    )
    private val member = admin.copy(uid = "m", roles = emptySet())
    private val today = LocalDate(2027, 2, 1)
    private val congress = SpecialEvent("1", "Kongress", LocalDate(2027, 6, 11), LocalDate(2027, 6, 13))

    private lateinit var events: FakeSpecialEventRepository
    private lateinit var eventTypes: FakeEventTypeRepository
    private val arena = EventType("kk", "Kreiskongress", cancelsTalk = true, icon = "ic_cp_arena_duo")
    private lateinit var viewModel: SpecialEventManagementViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        events = FakeSpecialEventRepository(listOf(congress))
        eventTypes = FakeEventTypeRepository(listOf(arena))
        val policy = SpecialEventPolicy()
        viewModel = SpecialEventManagementViewModel(
            ObserveManagedEventsUseCase(events, policy),
            SaveSpecialEventUseCase(events, policy),
            DeleteSpecialEventUseCase(events, policy),
            ObserveEventTypesUseCase(eventTypes, EventTypePolicy()),
            policy,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = viewModel.uiState.value

    @Test
    fun `the admin sees the events and creates a new one starting today`() {
        viewModel.onCurrentUserChanged(admin)
        assertTrue(state.canManage)
        assertEquals(listOf(congress), state.events)

        viewModel.newEvent(today)
        val editor = state.editor!!
        assertTrue(editor.isNew)
        assertEquals(today, editor.event.startDate)
        assertEquals(today, editor.event.endDate)

        viewModel.updateEditor(editor.copy(event = editor.event.copy(title = "Besuch")))
        viewModel.save()

        val saved = events.savedEvents.single()
        assertEquals("ev-1", saved.id)
        assertEquals(listOf(saved, congress), state.events)
        assertFalse(state.editor!!.isDirty)
    }

    @Test
    fun `validation errors keep the editor open`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.newEvent(today)
        viewModel.save()

        assertNotNull(state.errorMessage)
        assertNotNull(state.editor)
        assertFalse(state.isSaving)
        assertTrue(events.savedEvents.isEmpty())
    }

    @Test
    fun `deleting removes the event and closes its editor`() {
        viewModel.onCurrentUserChanged(admin)
        viewModel.editEvent(congress)
        viewModel.delete(congress)

        assertEquals(listOf("1"), events.deletedIds)
        assertTrue(state.events.isEmpty())
        assertNull(state.editor)
    }

    @Test
    fun `members without the admin role neither load nor edit`() {
        viewModel.onCurrentUserChanged(member)
        viewModel.newEvent(today)

        assertFalse(state.canManage)
        assertTrue(state.events.isEmpty())
        assertNull(state.editor)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun `a load error is shown and dismissing it loads again`() {
        events.observeError = IllegalStateException("offline")
        viewModel.onCurrentUserChanged(admin)
        assertEquals("offline", state.errorMessage)

        events.observeError = null
        viewModel.dismissError()
        assertNull(state.errorMessage)
        assertEquals(listOf(congress), state.events)
    }

    @Test
    fun `the editor keeps the dates in order and all exclusive`() {
        val editor = SpecialEventEditorState(congress)

        assertEquals(LocalDate(2027, 6, 20), editor.withStart(LocalDate(2027, 6, 20)).event.endDate)
        assertEquals(LocalDate(2027, 6, 13), editor.withStart(LocalDate(2027, 6, 12)).event.endDate)
        assertEquals(LocalDate(2027, 6, 1), editor.withEnd(LocalDate(2027, 6, 1)).event.startDate)

        val elders = editor.toggled(EventAudience.ELDERS)
        assertEquals(setOf(EventAudience.ELDERS), elders.event.audience)
        assertEquals(
            setOf(EventAudience.ELDERS, EventAudience.SISTERS),
            elders.toggled(EventAudience.SISTERS).event.audience,
        )
        assertEquals(emptySet(), elders.toggled(EventAudience.ELDERS).event.audience)
        assertEquals(setOf(EventAudience.ALL), elders.toggled(EventAudience.ALL).event.audience)
    }

    @Test
    fun `the event types load with the events and a failure keeps the events`() {
        viewModel.onCurrentUserChanged(admin)
        assertEquals(listOf(arena), state.eventTypes)

        eventTypes.observeError = IllegalStateException("rules not deployed")
        viewModel.onCurrentUserChanged(admin.copy(uid = "other"))
        assertEquals(listOf(congress), state.events)
        assertEquals(emptyList(), state.eventTypes)
        assertNull(state.errorMessage)
    }

    @Test
    fun `choosing a type fills an empty title and the icon follows the type`() {
        val blank = SpecialEventEditorState(SpecialEvent(startDate = LocalDate(2027, 3, 6)))

        val typed = blank.withType(arena)
        assertEquals("kk", typed.event.eventTypeId)
        assertEquals("Kreiskongress", typed.event.title)
        assertEquals("Frühjahr", blank.copy(event = blank.event.copy(title = "Frühjahr")).withType(arena).event.title)
        assertEquals(null, typed.withType(null).event.eventTypeId)
        assertEquals("Kreiskongress", typed.withType(null).event.title)

        assertEquals("ic_cp_arena_duo", specialEventIconName(typed.event, listOf(arena)))
        assertNull(specialEventIconName(typed.event, emptyList()))
        assertNull(specialEventIconName(blank.event, listOf(arena)))
    }

    @Test
    fun `texts show the date range and the groups`() {
        assertEquals("Fr, 11.06.2027 – So, 13.06.2027", congress.dateRangeText())
        assertEquals("Fr, 11.06.2027", congress.copy(endDate = congress.startDate).dateRangeText())
        assertEquals(
            "Brüder, Älteste",
            congress.copy(audience = setOf(EventAudience.ELDERS, EventAudience.BROTHERS)).audienceText(),
        )
    }
}
