package de.geosphere.congregationplaner.presentation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TalkPlanningTabsTest {
    private val visible = listOf(
        CongregationSection.USER_MANAGEMENT,
        CongregationSection.WEEKEND_TALKS,
        CongregationSection.TALK_HISTORY,
    )

    @Test
    fun `a talk section on the phone shows the visible talk sections as tabs`() {
        val tabs = talkPlanningTabs(visible, CongregationSection.TALK_HISTORY, isWide = false)

        assertTrue(tabs.isActive)
        assertEquals(listOf(CongregationSection.WEEKEND_TALKS, CongregationSection.TALK_HISTORY), tabs.sections)
        assertEquals(listOf(false, true), tabs.labels.map { it.second })
    }

    @Test
    fun `wide windows and other sections have no tabs`() {
        assertFalse(talkPlanningTabs(visible, CongregationSection.WEEKEND_TALKS, isWide = true).isActive)
        assertFalse(talkPlanningTabs(visible, CongregationSection.USER_MANAGEMENT, isWide = false).isActive)
        assertFalse(talkPlanningTabs(visible, null, isWide = false).isActive)
    }
}
