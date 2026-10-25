package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkCatalogUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkHistoryUseCase
import de.geosphere.congregationplaner.talk.testing.FakeTalkCatalogRepository
import de.geosphere.congregationplaner.talk.testing.FakeTalkHistoryRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserRole
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
class TalkHistoryViewModelTest {
    private val planner = AppUser(
        uid = "planner",
        approved = true,
        role = UserRole.CONGREGATION_ADMIN,
        congregation = testCongregation,
    )
    private val member = AppUser(uid = "member", approved = true, congregation = testCongregation)

    private val first = Talk(1, "Erster Vortrag")
    private val second = Talk(2, "Zweiter Vortrag")
    private val spring = LocalDate(2026, 3, 1)
    private val autumn = LocalDate(2026, 10, 25)

    private lateinit var catalog: FakeTalkCatalogRepository
    private lateinit var history: FakeTalkHistoryRepository
    private lateinit var viewModel: TalkHistoryViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        catalog = FakeTalkCatalogRepository(listOf(second, first))
        history = FakeTalkHistoryRepository(listOf(TalkHistoryEntry(1, listOf(spring, autumn))))
        val policy = TalkSchedulePermissionPolicy()
        viewModel = TalkHistoryViewModel(
            ObserveTalkCatalogUseCase(catalog),
            ObserveTalkHistoryUseCase(history),
            policy,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = viewModel.uiState.value

    @Test
    fun `planner sees every talk with its last held day or none`() {
        viewModel.onCurrentUserChanged(planner)

        assertTrue(state.canPlan)
        assertFalse(state.isLoading)
        assertEquals(listOf(first, second), state.rows.map { it.talk })
        assertEquals(autumn, state.rows[0].entry?.lastHeldOn)
        assertNull(state.rows[1].entry)
    }

    @Test
    fun `other users do not load anything`() {
        viewModel.onCurrentUserChanged(member)
        viewModel.selectTalk(first)

        assertFalse(state.canPlan)
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

        history.entries.value = history.entries.value + TalkHistoryEntry(2, listOf(autumn), listOf(autumn))

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
}
