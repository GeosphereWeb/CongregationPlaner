package de.geosphere.congregationplaner.home.presentation

import de.geosphere.congregationplaner.event.domain.model.SpecialEvent
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeTextsTest {
    @Test
    fun `the talk section names the helper role`() {
        assertEquals("Als Vortragsplaner", talkSectionLabel(asHelper = false))
        assertEquals("Als Vortragsplanungs-Helfer", talkSectionLabel(asHelper = true))
    }

    @Test
    fun `event dates name a day a range within a month or a range across months`() {
        fun event(start: LocalDate, end: LocalDate = start) = SpecialEvent("1", "K", start, end)

        assertEquals("Fr, 11. Juni", eventDatesText(event(LocalDate(2027, 6, 11))))
        assertEquals("11.–13. Juni", eventDatesText(event(LocalDate(2027, 6, 11), LocalDate(2027, 6, 13))))
        assertEquals("30. Mai – 2. Juni", eventDatesText(event(LocalDate(2027, 5, 30), LocalDate(2027, 6, 2))))
        assertEquals(
            "30. Dezember – 2. Januar",
            eventDatesText(event(LocalDate(2027, 12, 30), LocalDate(2028, 1, 2))),
        )
    }

    @Test
    fun `the next talk gets the informed badge only once the speaker is informed`() {
        val slot = TalkSlot(LocalDate(2026, 11, 22), LocalTime(17, 30))

        assertNull(nextTalkInformedBadge(slot))
        assertEquals("Informiert", nextTalkInformedBadge(slot.copy(confirmed = true)))
    }

    @Test
    fun `the next talk shows its number with the title on the next line`() {
        val slot = TalkSlot(LocalDate(2026, 11, 22), LocalTime(17, 30))

        assertEquals("Noch nicht geplant", nextTalkText(slot))
        assertEquals("Vortrag Nr. 73", nextTalkText(slot.copy(talkNumber = 73)))
        assertEquals(
            "Vortrag Nr. 73\nEin weises Herz",
            nextTalkText(slot.copy(talkNumber = 73, talkTitle = "Ein weises Herz")),
        )
    }
}
