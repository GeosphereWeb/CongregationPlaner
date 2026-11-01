package de.geosphere.congregationplaner.presentation.navigation

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppNavigationViewModelTest {
    private val viewModel = AppNavigationViewModel(navigationAccess())

    private val planner = AppUser(uid = "p", approved = true, roles = setOf(CongregationRole.TALK_PLANNER))
    private val admin = AppUser(uid = "a", approved = true, roles = setOf(CongregationRole.CONGREGATION_ADMIN))

    private val state get() = viewModel.uiState.value

    @Test
    fun `starts at home without a user`() {
        assertEquals(TopLevelDestination.HOME, state.destination)
        assertEquals(VisibleNavigation(), state.visible)
    }

    @Test
    fun `selecting a visible area opens its overview`() {
        viewModel.onUserChanged(planner)

        viewModel.openCongregationSection(CongregationSection.TALK_HISTORY)
        viewModel.select(TopLevelDestination.CONGREGATION)

        assertEquals(TopLevelDestination.CONGREGATION, state.destination)
        assertNull(state.congregationSection)
    }

    @Test
    fun `an area the user may not see cannot be selected`() {
        viewModel.onUserChanged(planner)

        viewModel.select(TopLevelDestination.GLOBAL_ADMIN)
        assertEquals(TopLevelDestination.HOME, state.destination)

        viewModel.openCongregationSection(CongregationSection.USER_MANAGEMENT)
        viewModel.openGlobalSection(GlobalSection.CIRCUITS)
        assertEquals(TopLevelDestination.HOME, state.destination)
        assertNull(state.congregationSection)
        assertNull(state.globalSection)
    }

    @Test
    fun `opening a section selects its area`() {
        viewModel.onUserChanged(admin)

        viewModel.openCongregationSection(CongregationSection.USER_MANAGEMENT)

        assertEquals(TopLevelDestination.CONGREGATION, state.destination)
        assertEquals(CongregationSection.USER_MANAGEMENT, state.congregationSection)

        viewModel.openCongregationSection(null)
        assertNull(state.congregationSection)
    }

    @Test
    fun `losing a role falls back to home`() {
        viewModel.onUserChanged(planner)
        viewModel.openCongregationSection(CongregationSection.WEEKEND_TALKS)

        viewModel.onUserChanged(planner.copy(roles = emptySet()))

        assertEquals(TopLevelDestination.HOME, state.destination)
        assertNull(state.congregationSection)
    }

    @Test
    fun `losing only the open section keeps the area with its overview`() {
        val both = admin.copy(roles = setOf(CongregationRole.CONGREGATION_ADMIN, CongregationRole.TALK_PLANNER))
        viewModel.onUserChanged(both)
        viewModel.openCongregationSection(CongregationSection.TALK_HISTORY)

        viewModel.onUserChanged(admin)

        assertEquals(TopLevelDestination.CONGREGATION, state.destination)
        assertNull(state.congregationSection)
    }

    @Test
    fun `signing out resets the navigation`() {
        viewModel.onUserChanged(admin)
        viewModel.openCongregationSection(CongregationSection.GENERAL_DATA)

        viewModel.onUserChanged(null)

        assertEquals(AppNavigationState(), state)
    }

    @Test
    fun `a global admin opens global sections`() {
        viewModel.onUserChanged(AppUser(uid = "g", approved = true, isGlobalAdmin = true))

        viewModel.openGlobalSection(GlobalSection.TALK_REFERENCE)

        assertEquals(TopLevelDestination.GLOBAL_ADMIN, state.destination)
        assertEquals(GlobalSection.TALK_REFERENCE, state.globalSection)
    }
}
