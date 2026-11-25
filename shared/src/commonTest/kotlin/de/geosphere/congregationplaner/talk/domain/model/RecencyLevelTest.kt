package de.geosphere.congregationplaner.talk.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class RecencyLevelTest {
    private val today = LocalDate(2026, 11, 25)

    private fun level(lastHeld: LocalDate?) = recencyLevelOf(lastHeld, today)

    @Test
    fun `a talk never held has no recency`() {
        assertEquals(RecencyLevel.NEVER, level(null))
    }

    @Test
    fun `a planned talk and one held within a year are recent`() {
        assertEquals(RecencyLevel.RECENT, level(LocalDate(2026, 12, 6)))
        assertEquals(RecencyLevel.RECENT, level(today))
        assertEquals(RecencyLevel.RECENT, level(LocalDate(2025, 11, 26)))
    }

    @Test
    fun `a talk exactly on a boundary falls into the older level`() {
        assertEquals(RecencyLevel.ONE_TO_TWO_YEARS, level(LocalDate(2025, 11, 25)))
        assertEquals(RecencyLevel.TWO_TO_FOUR_YEARS, level(LocalDate(2024, 11, 25)))
        assertEquals(RecencyLevel.FOUR_TO_SIX_YEARS, level(LocalDate(2022, 11, 25)))
        assertEquals(RecencyLevel.OVER_SIX_YEARS, level(LocalDate(2020, 11, 25)))
    }

    @Test
    fun `a day before a boundary stays in the newer level`() {
        assertEquals(RecencyLevel.TWO_TO_FOUR_YEARS, level(LocalDate(2022, 11, 26)))
    }

    @Test
    fun `full years are counted across a leap day`() {
        assertEquals(RecencyLevel.TWO_TO_FOUR_YEARS, recencyLevelOf(LocalDate(2020, 2, 29), LocalDate(2024, 2, 28)))
    }
}
