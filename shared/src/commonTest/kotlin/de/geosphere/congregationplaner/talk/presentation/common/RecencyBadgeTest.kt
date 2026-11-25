package de.geosphere.congregationplaner.talk.presentation.common

import de.geosphere.congregationplaner.talk.domain.model.RecencyLevel
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class RecencyBadgeTest {
    private val today = LocalDate(2026, 11, 25)

    private fun badge(lastHeld: LocalDate?, count: Int) = recencyBadgeOf(lastHeld, count, today)

    @Test
    fun `a talk never held shows no count`() {
        assertEquals(RecencyBadge(RecencyLevel.NEVER, "nie"), badge(null, 0))
    }

    @Test
    fun `a talk planned for a later day is marked as planned`() {
        assertEquals(RecencyBadge(RecencyLevel.RECENT, "geplant · 1×"), badge(LocalDate(2026, 12, 6), 1))
    }

    @Test
    fun `a talk held within the last seven days is this week`() {
        assertEquals("diese Woche · 4×", badge(today, 4).label)
        assertEquals("diese Woche · 4×", badge(LocalDate(2026, 11, 19), 4).label)
    }

    @Test
    fun `within a month the distance is in weeks`() {
        assertEquals("vor 2 Wo. · 3×", badge(LocalDate(2026, 11, 11), 3).label)
    }

    @Test
    fun `within a year the distance is in months`() {
        assertEquals("vor 8 Mon. · 2×", badge(LocalDate(2026, 3, 25), 2).label)
    }

    @Test
    fun `after a year the distance is in years`() {
        assertEquals(RecencyBadge(RecencyLevel.FOUR_TO_SIX_YEARS, "vor 5 J. · 2×"), badge(LocalDate(2021, 11, 2), 2))
    }
}
