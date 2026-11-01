package de.geosphere.congregationplaner.home.presentation

import de.geosphere.congregationplaner.home.domain.model.TalkSummary
import de.geosphere.congregationplaner.home.domain.usecase.ObserveHomeSummaryUseCase
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkYearUseCase
import de.geosphere.congregationplaner.talk.testing.FakeTalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.usecase.ObserveMembersUseCase
import de.geosphere.congregationplaner.user.testing.FakeUserRepository
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val today = LocalDate(2026, 11, 1)
    private val planner = AppUser(
        uid = "p",
        approved = true,
        roles = setOf(CongregationRole.TALK_PLANNER, CongregationRole.CONGREGATION_ADMIN),
        congregation = testCongregation,
    )

    private lateinit var schedule: FakeTalkScheduleRepository
    private lateinit var users: FakeUserRepository
    private lateinit var viewModel: HomeViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        schedule = FakeTalkScheduleRepository()
        users = FakeUserRepository()
        viewModel = HomeViewModel(
            ObserveHomeSummaryUseCase(
                TalkSchedulePermissionPolicy(),
                UserPermissionPolicy(),
                ObserveTalkYearUseCase(schedule),
                ObserveMembersUseCase(users),
            ),
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = viewModel.uiState.value

    @Test
    fun `starts empty`() {
        assertEquals(HomeUiState(), state)
    }

    @Test
    fun `a planner and admin gets both blocks`() {
        users.members.value = listOf(AppUser(uid = "a", approved = false))

        viewModel.onCurrentUserChanged(planner, today)

        assertEquals(TalkSummary(), state.summary.talk)
        assertEquals(1, state.summary.pendingApprovals)
    }

    @Test
    fun `the blocks follow changes of the data`() {
        viewModel.onCurrentUserChanged(planner, today)

        schedule.slots.value = listOf(TalkSlot(LocalDate(2026, 11, 8), LocalTime(17, 30)))

        assertEquals(LocalDate(2026, 11, 8), state.summary.talk?.nextSlot?.date)
        assertEquals(listOf(LocalDate(2026, 11, 8)), state.summary.talk?.openDates)
    }

    @Test
    fun `signing out clears the blocks`() {
        viewModel.onCurrentUserChanged(planner, today)

        viewModel.onCurrentUserChanged(null, today)

        assertEquals(HomeUiState(), state)
    }

    @Test
    fun `losing the role removes the block`() {
        viewModel.onCurrentUserChanged(planner, today)

        viewModel.onCurrentUserChanged(planner.copy(roles = setOf(CongregationRole.CONGREGATION_ADMIN)), today)

        assertNull(state.summary.talk)
        assertNotNull(state.summary.pendingApprovals)
    }

    @Test
    fun `a load error is shown and ends the observation`() {
        schedule.observeError = IllegalStateException("permission denied")

        viewModel.onCurrentUserChanged(planner, today)

        assertEquals("permission denied", state.errorMessage)
    }
}
