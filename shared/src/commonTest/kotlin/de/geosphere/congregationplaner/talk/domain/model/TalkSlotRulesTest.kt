package de.geosphere.congregationplaner.talk.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TalkSlotRulesTest {
    @Test
    fun `a year has 52 sundays from the first to the last`() {
        val dates = datesOfWeekdayIn(2026, weekday = 7)

        assertEquals(52, dates.size)
        assertEquals(LocalDate(2026, 1, 4), dates.first())
        assertEquals(LocalDate(2026, 12, 27), dates.last())
    }

    @Test
    fun `a weekday that starts and ends the year occurs 53 times`() {
        val dates = datesOfWeekdayIn(2026, weekday = 4)

        assertEquals(53, dates.size)
        assertEquals(LocalDate(2026, 1, 1), dates.first())
        assertEquals(LocalDate(2026, 12, 31), dates.last())
    }

    @Test
    fun `every date lies in the year and one week apart`() {
        val dates = datesOfWeekdayIn(2028, weekday = 6)

        assertTrue(dates.all { it.year == 2028 })
        assertTrue(dates.zipWithNext().all { (a, b) -> b.toEpochDays() - a.toEpochDays() == 7L })
    }

    @Test
    fun `a weekday outside 1 to 7 is rejected`() {
        assertFailsWith<IllegalArgumentException> { datesOfWeekdayIn(2026, weekday = 0) }
        assertFailsWith<IllegalArgumentException> { datesOfWeekdayIn(2026, weekday = 8) }
    }
}
