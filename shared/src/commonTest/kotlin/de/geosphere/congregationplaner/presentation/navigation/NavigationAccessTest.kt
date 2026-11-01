package de.geosphere.congregationplaner.presentation.navigation

import de.geosphere.congregationplaner.talk.domain.permission.TalkReferenceDataPolicy
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.permission.CircuitPermissionPolicy
import de.geosphere.congregationplaner.user.domain.permission.CongregationPermissionPolicy
import de.geosphere.congregationplaner.user.domain.permission.MidweekPermissionPolicy
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import kotlin.test.Test
import kotlin.test.assertEquals

internal fun navigationAccess() = NavigationAccess(
    UserPermissionPolicy(),
    CongregationPermissionPolicy(),
    TalkSchedulePermissionPolicy(),
    MidweekPermissionPolicy(),
    CircuitPermissionPolicy(),
    TalkReferenceDataPolicy(),
)

class NavigationAccessTest {
    private val access = navigationAccess()

    private fun member(vararg roles: CongregationRole) = AppUser(uid = "u", approved = true, roles = roles.toSet())

    @Test
    fun `a member without role sees only home and settings`() {
        val visible = access.visibleFor(member())

        assertEquals(listOf(TopLevelDestination.HOME, TopLevelDestination.SETTINGS), visible.topLevel)
        assertEquals(emptyList(), visible.congregationSections)
        assertEquals(emptyList(), visible.globalSections)
    }

    @Test
    fun `a talk planner sees only the talk planning`() {
        val visible = access.visibleFor(member(CongregationRole.TALK_PLANNER))

        assertEquals(
            listOf(TopLevelDestination.HOME, TopLevelDestination.CONGREGATION, TopLevelDestination.SETTINGS),
            visible.topLevel,
        )
        assertEquals(CongregationSection.talkPlanning, visible.congregationSections)
    }

    @Test
    fun `a task planner sees only the midweek planning`() {
        val visible = access.visibleFor(member(CongregationRole.TASK_PLANNER))

        assertEquals(listOf(CongregationSection.MIDWEEK_PLANNING), visible.congregationSections)
    }

    @Test
    fun `an admin sees the administration but no planning`() {
        val visible = access.visibleFor(member(CongregationRole.CONGREGATION_ADMIN))

        assertEquals(
            listOf(CongregationSection.GENERAL_DATA, CongregationSection.USER_MANAGEMENT),
            visible.congregationSections,
        )
        assertEquals(emptyList(), visible.globalSections)
    }

    @Test
    fun `an admin who is also talk planner sees administration and talk planning`() {
        val visible = access.visibleFor(
            member(CongregationRole.CONGREGATION_ADMIN, CongregationRole.TALK_PLANNER),
        )

        assertEquals(
            listOf(CongregationSection.GENERAL_DATA, CongregationSection.USER_MANAGEMENT) +
                CongregationSection.talkPlanning,
            visible.congregationSections,
        )
    }

    @Test
    fun `all roles together show every congregation section`() {
        val visible = access.visibleFor(member(*CongregationRole.entries.toTypedArray()))

        assertEquals(CongregationSection.entries, visible.congregationSections)
        assertEquals(emptyList(), visible.globalSections)
    }

    @Test
    fun `a global admin additionally sees the global administration`() {
        val visible = access.visibleFor(AppUser(uid = "g", approved = true, isGlobalAdmin = true))

        assertEquals(
            listOf(
                TopLevelDestination.HOME,
                TopLevelDestination.CONGREGATION,
                TopLevelDestination.GLOBAL_ADMIN,
                TopLevelDestination.SETTINGS,
            ),
            visible.topLevel,
        )
        assertEquals(GlobalSection.entries, visible.globalSections)
        // Heute behalten globale Admins ihre Rechte: Verwaltung und Vortragsplanung, aber keine Wochenplanung.
        assertEquals(CongregationSection.entries - CongregationSection.MIDWEEK_PLANNING, visible.congregationSections)
    }

    @Test
    fun `an unapproved admin sees nothing but home and settings`() {
        val visible = access.visibleFor(member(CongregationRole.CONGREGATION_ADMIN).copy(approved = false))

        assertEquals(listOf(TopLevelDestination.HOME, TopLevelDestination.SETTINGS), visible.topLevel)
    }
}
