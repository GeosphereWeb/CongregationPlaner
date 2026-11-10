package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SlotListingTest {
    private fun slot(day: LocalDate, note: String? = null) = TalkSlot(date = day, time = LocalTime(10, 0), note = note)

    @Test
    fun `slots are grouped by month in list order`() {
        val slots = listOf(
            slot(LocalDate(2026, 1, 4)),
            slot(LocalDate(2026, 1, 11)),
            slot(LocalDate(2026, 2, 1)),
        )

        assertEquals(listOf(slots.take(2), slots.drop(2)), slotsByMonth(slots))
        assertEquals(emptyList(), slotsByMonth(emptyList()))
    }

    @Test
    fun `the note is cut to the limit and an empty note is dropped`() {
        val day = LocalDate(2026, 1, 4)
        val long = "x".repeat(TalkSlot.MAX_NOTE_LENGTH + 5)

        assertEquals(TalkSlot.MAX_NOTE_LENGTH, slot(day, long).withLimitedNote().note?.length)
        assertNull(slot(day, "").withLimitedNote().note)
        assertEquals("kurz", slot(day, "kurz").withLimitedNote().note)
    }
}
