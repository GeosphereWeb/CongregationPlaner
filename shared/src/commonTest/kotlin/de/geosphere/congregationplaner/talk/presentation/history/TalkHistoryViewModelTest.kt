package de.geosphere.congregationplaner.talk.presentation.history

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.BuildTalkHistoryOverviewUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkCatalogUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkHistoryUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SearchSpeakerHistoryUseCase
import de.geosphere.congregationplaner.talk.testing.FakeTalkCatalogRepository
import de.geosphere.congregationplaner.talk.testing.FakeTalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.testing.testCongregation
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
class TalkHistoryViewModelTest {
    private val planner = AppUser(
        uid = "planner",
        approved = true,
        roles = setOf(CongregationRole.CONGREGATION_ADMIN, CongregationRole.TALK_PLANNER),
        congregation = testCongregation,
    )
    private val helper = planner.copy(uid = "helper", roles = setOf(CongregationRole.TALK_HELPER))
    private val member = AppUser(uid = "member", approved = true, congregation = testCongregation)

    private val first = Talk(1, "Erster Vortrag")
    private val second = Talk(2, "Zweiter Vortrag")
    private val spring = LocalDate(2026, 3, 1)
    private val autumn = LocalDate(2026, 10, 25)

    private lateinit var catalog: FakeTalkCatalogRepository
    private lateinit var schedule: FakeTalkScheduleRepository
    private lateinit var viewModel: TalkHistoryViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        catalog = FakeTalkCatalogRepository(listOf(second, first))
        schedule = FakeTalkScheduleRepository(listOf(held(1, spring), held(1, autumn)))
        val policy = TalkSchedulePermissionPolicy()
        viewModel = TalkHistoryViewModel(
            ObserveTalkCatalogUseCase(catalog),
            ObserveTalkHistoryUseCase(schedule),
            policy,
            BuildTalkHistoryOverviewUseCase(),
            SearchSpeakerHistoryUseCase(),
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = viewModel.uiState.value

    private fun held(number: Int, day: LocalDate, lastName: String? = null, firstName: String? = null) = TalkSlot(
        date = day,
        time = LocalTime(10, 0),
        talkNumber = number,
        talkTitle = "Vortrag $number",
        speakerMemberId = lastName?.let { "m-$it" },
        speakerLastName = lastName,
        speakerFirstName = firstName,
    )

    @Test
    fun `planner sees every talk with its last held day or none`() {
        viewModel.onCurrentUserChanged(planner)

        assertTrue(state.canView)
        assertFalse(state.isLoading)
        assertEquals(listOf(first, second), state.rows.map { it.talk })
        assertEquals(autumn, state.rows[0].entry?.lastHeldOn)
        assertNull(state.rows[1].entry)
    }

    @Test
    fun `the list is sorted by number and grouping is off until switched on`() {
        val unsorted = BuildTalkHistoryOverviewUseCase()(listOf(Talk(3, "C"), Talk(1, "A"), Talk(2, "B")), emptyList())

        assertEquals(listOf(1, 2, 3), TalkHistoryUiState(overview = unsorted).rows.map { it.talk.number })
        assertFalse(state.groupByFrequency)

        viewModel.setGroupByFrequency(true)
        assertTrue(state.groupByFrequency)
        viewModel.setGroupByFrequency(false)
        assertFalse(state.groupByFrequency)
    }

    @Test
    fun `a talk helper sees the history`() {
        viewModel.onCurrentUserChanged(helper)
        viewModel.selectTalk(first)

        assertTrue(state.canView)
        assertEquals(2, state.rows.size)
        assertNotNull(state.selected)
    }

    @Test
    fun `dismissing an error keeps the helper's history`() {
        viewModel.onCurrentUserChanged(helper)
        viewModel.dismissError()

        assertEquals(2, state.rows.size)
    }

    @Test
    fun `other users do not load anything`() {
        viewModel.onCurrentUserChanged(member)
        viewModel.selectTalk(first)

        assertFalse(state.canView)
        assertTrue(state.rows.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.selected)
    }

    @Test
    fun `selecting a talk shows its days and closing the detail clears the selection`() {
        viewModel.onCurrentUserChanged(planner)
        assertFalse(state.hasDetail)

        viewModel.selectTalk(first)
        assertTrue(state.hasDetail)
        assertEquals(listOf(spring, autumn), state.selected?.entry?.heldOn)
        assertFalse(state.isSaving)

        viewModel.closeDetail()
        assertFalse(state.hasDetail)
        assertNull(state.selected)
    }

    @Test
    fun `the detail follows changes made by the planning`() {
        viewModel.onCurrentUserChanged(planner)
        viewModel.selectTalk(second)
        assertNull(state.selected?.entry)

        schedule.slots.value = schedule.slots.value + held(2, autumn)

        assertEquals(listOf(autumn), state.selected?.entry?.heldOn)
    }

    @Test
    fun `a load error is retried when dismissed`() {
        catalog.observeError = IllegalStateException("denied")
        viewModel.onCurrentUserChanged(planner)
        assertEquals("denied", state.errorMessage)
        assertFalse(state.isLoading)

        catalog.observeError = null
        viewModel.dismissError()

        assertNull(state.errorMessage)
        assertEquals(2, state.rows.size)
    }

    @Test
    fun `a load error without a message falls back to a default text`() {
        catalog.observeError = IllegalStateException()

        viewModel.onCurrentUserChanged(planner)

        assertNotNull(state.errorMessage)
    }

    @Test
    fun `losing the permission clears the list and the selection`() {
        viewModel.onCurrentUserChanged(planner)
        viewModel.selectTalk(first)

        viewModel.onCurrentUserChanged(member)

        assertTrue(state.rows.isEmpty())
        assertNull(state.selected)
    }

    @Test
    fun `a talk can be selected by its number`() {
        viewModel.onCurrentUserChanged(planner)

        viewModel.selectTalkNumber(first.number)

        assertEquals(first, state.selected?.talk)
    }

    @Test
    fun `groups follow the frequency and the speaker search applies at once`() {
        schedule.slots.value = listOf(
            held(1, LocalDate(2025, 3, 2)),
            held(1, autumn, "Muster", "Max"),
            held(2, spring, "Berg", "Lea"),
        )
        viewModel.onCurrentUserChanged(planner)

        assertEquals(listOf(2, 1), viewModel.uiState.value.groups.map { it.count })
        assertFalse(viewModel.uiState.value.isSearching)

        viewModel.search("m")
        assertFalse(viewModel.uiState.value.isSearching)
        assertTrue(viewModel.uiState.value.speakerResults.isEmpty())

        viewModel.search("mus")
        assertTrue(viewModel.uiState.value.isSearching)
        assertEquals(listOf("Muster, Max"), viewModel.uiState.value.speakerResults.map { it.speaker })
        assertEquals(listOf(1), viewModel.uiState.value.speakerResults.single().talks.map { it.talkNumber })
    }
}
