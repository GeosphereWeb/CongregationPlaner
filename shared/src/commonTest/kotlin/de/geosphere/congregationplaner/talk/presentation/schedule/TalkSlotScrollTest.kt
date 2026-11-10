package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TalkSlotScrollTest {
    private fun slot(month: Int, day: Int) = TalkSlot(LocalDate(2026, month, day), LocalTime(17, 30))

    private val slots = listOf(slot(1, 4), slot(1, 11), slot(2, 1), slot(2, 8), slot(3, 1))

    @Test
    fun `index counts the month headers in front of the slot`() {
        // header Jan, 4.1., 11.1., header Feb, 1.2., 8.2., header Mar, 1.3.
        assertEquals(1, slotListIndex(slots, LocalDate(2026, 1, 1)))
        assertEquals(2, slotListIndex(slots, LocalDate(2026, 1, 11)))
        assertEquals(4, slotListIndex(slots, LocalDate(2026, 2, 1)))
        assertEquals(7, slotListIndex(slots, LocalDate(2026, 3, 1)))
    }

    @Test
    fun `a day between two slots points at the next one`() {
        assertEquals(5, slotListIndex(slots, LocalDate(2026, 2, 2)))
    }

    @Test
    fun `there is no index without a slot on or after the day`() {
        assertNull(slotListIndex(slots, LocalDate(2026, 3, 2)))
        assertNull(slotListIndex(emptyList(), LocalDate(2026, 1, 1)))
    }
}
