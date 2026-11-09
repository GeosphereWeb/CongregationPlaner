package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.ui.chip.StatusTone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TalkSlotPresentationTest {
    private val today = LocalDate(2026, 11, 1)
    private val time = LocalTime(17, 30)

    private fun slot(date: LocalDate = today.plusDays(7), block: (TalkSlot) -> TalkSlot = { it }) =
        block(TalkSlot(date, time))

    private fun LocalDate.plusDays(days: Int) = LocalDate.fromEpochDays(toEpochDays() + days)

    private val withTalk = { s: TalkSlot -> s.copy(talkNumber = 12, talkTitle = "Der Weg zum Glück") }
    private val withMember = { s: TalkSlot ->
        withTalk(s).copy(speakerMemberId = "m1", speakerLastName = "Muster", speakerFirstName = "Max")
    }

    @Test
    fun `next date is the first upcoming slot that is not cancelled`() {
        val slots = listOf(
            slot(today.plusDays(-7)),
            slot(today) { it.copy(talkCancelled = true) },
            slot(today.plusDays(7)),
            slot(today.plusDays(14)),
        )

        assertEquals(today.plusDays(7), nextSlotDate(slots, today))
    }

    @Test
    fun `there is no next date without an upcoming slot`() {
        assertNull(nextSlotDate(listOf(slot(today.plusDays(-7))), today))
        assertNull(nextSlotDate(emptyList(), today))
    }

    @Test
    fun `cancelled wins over next`() {
        val cancelled = slot(today) { it.copy(talkCancelled = true) }

        assertEquals(SlotStatus.CANCELLED, slotStatus(cancelled, nextSlotDate = today))
    }

    @Test
    fun `next wins over completeness`() {
        assertEquals(SlotStatus.NEXT, slotStatus(slot(today), nextSlotDate = today))
    }

    @Test
    fun `without a talk the slot is not planned`() {
        assertEquals(SlotStatus.NOT_PLANNED, slotStatus(slot(), nextSlotDate = null))
    }

    @Test
    fun `talk and speaker are planned together so one alone is not planned`() {
        val speakerOnly = slot { it.copy(speakerMemberId = "m1", speakerLastName = "Muster", speakerFirstName = "Max") }

        assertEquals(SlotStatus.NOT_PLANNED, slotStatus(speakerOnly, nextSlotDate = null))
        assertEquals(SlotStatus.NOT_PLANNED, slotStatus(slot(block = withTalk), nextSlotDate = null))
    }

    @Test
    fun `talk and speaker make the slot planned and a reminded speaker makes it informed`() {
        assertEquals(SlotStatus.PLANNED, slotStatus(slot(block = withMember), nextSlotDate = null))
        assertEquals(
            SlotStatus.INFORMED,
            slotStatus(slot { withMember(it).copy(confirmed = true) }, nextSlotDate = null),
        )
    }

    @Test
    fun `an external speaker counts as a speaker`() {
        val s = slot {
            withTalk(it).copy(
                externalSpeakerId = "e1",
                speakerLastName = "Extern",
                speakerFirstName = "Eva",
                speakerCongregation = "Nord",
            )
        }

        assertEquals(SlotStatus.PLANNED, slotStatus(s, nextSlotDate = null))
    }

    @Test
    fun `chips follow the design`() {
        val s = slot()

        assertEquals(SlotChip("Nächster", StatusTone.Gold), slotChip(s, SlotStatus.NEXT))
        assertEquals(SlotChip("Entfällt", StatusTone.Sky), slotChip(s, SlotStatus.CANCELLED))
        assertEquals(SlotChip("Nicht geplant", StatusTone.Coral), slotChip(s, SlotStatus.NOT_PLANNED))
        assertEquals(SlotChip("Geplant", StatusTone.Sky), slotChip(s, SlotStatus.PLANNED))
        assertEquals(SlotChip("Informiert", StatusTone.Mint), slotChip(s, SlotStatus.INFORMED))
    }

    @Test
    fun `counts ignore the next marker and cover every planning status`() {
        val slots = listOf(
            slot(today),
            slot(today.plusDays(7), block = withMember),
            slot(today.plusDays(14)) { withMember(it).copy(confirmed = true) },
            slot(today.plusDays(21)) { it.copy(talkCancelled = true) },
            slot(today.plusDays(28)),
        )

        assertEquals(
            mapOf(
                SlotStatus.NOT_PLANNED to 2,
                SlotStatus.PLANNED to 2,
                SlotStatus.CANCELLED to 1,
            ),
            statusCounts(slots),
        )
    }

    @Test
    fun `an empty filter shows all slots and several statuses add up`() {
        val open = slot(today)
        val planned = slot(today.plusDays(7), block = withMember)
        val informed = slot(today.plusDays(14)) { withMember(it).copy(confirmed = true) }
        val cancelled = slot(today.plusDays(21)) { it.copy(talkCancelled = true) }
        val all = listOf(open, planned, informed, cancelled)

        assertEquals(all, filterSlots(all, emptySet()))
        // Informierte Termine gehören zum Filter „Geplant“.
        assertEquals(listOf(planned, informed), filterSlots(all, setOf(SlotStatus.PLANNED)))
        assertEquals(listOf(open, cancelled), filterSlots(all, setOf(SlotStatus.NOT_PLANNED, SlotStatus.CANCELLED)))
    }

    @Test
    fun `filter labels follow the planning terms`() {
        assertEquals(
            listOf("Nicht geplant", "Geplant", "Entfällt"),
            FILTER_STATUSES.map(::statusLabel),
        )
    }

    @Test
    fun `the week event name is always visible either as its own line or in the chip`() {
        val withEvent = slot { it.copy(weekEventId = "besuch", weekEventName = "Dienstwoche") }
        val cancelled = slot { it.copy(weekEventId = "kongress", weekEventName = "Kongress", talkCancelled = true) }

        assertEquals("Dienstwoche", slotEventLine(withEvent))
        assertNull(slotEventLine(cancelled))
        assertEquals("Kongress", slotChip(cancelled, SlotStatus.CANCELLED).text)
        assertNull(slotEventLine(slot()))
    }

    @Test
    fun `a cancelled slot shows the week event name`() {
        val s = slot { it.copy(weekEventId = "kongress", weekEventName = "Kongress", talkCancelled = true) }

        assertEquals(SlotChip("Kongress", StatusTone.Sky), slotChip(s, SlotStatus.CANCELLED))
    }

    @Test
    fun `title shows the talk number and topic or that nothing is planned`() {
        assertEquals("Vortrag Nr. 12 – Der Weg zum Glück", slotTitle(slot(block = withTalk)))
        assertEquals("Vortrag Nr. 12", slotTitle(slot { it.copy(talkNumber = 12, talkTitle = null) }))
        assertEquals("Noch nicht geplant", slotTitle(slot()))
    }

    @Test
    fun `speaker line names the origin`() {
        assertEquals("Muster, Max · eigene Versammlung", slotSpeakerLine(slot(block = withMember)))
        assertEquals("Redner noch offen", slotSpeakerLine(slot()))
        val external = slot {
            it.copy(
                externalSpeakerId = "e1",
                speakerLastName = "Extern",
                speakerFirstName = "Eva",
                speakerCongregation = "Nord",
            )
        }
        assertEquals("Extern, Eva · Gast aus Nord", slotSpeakerLine(external))
    }

    @Test
    fun `month label has month and year`() {
        assertEquals("November 2026", slotMonthLabel(LocalDate(2026, 11, 1)))
    }

    @Test
    fun `headline has day and month and year`() {
        assertEquals("1. November 2026", slotDateHeadline(LocalDate(2026, 11, 1)))
    }

    @Test
    fun `current week runs from monday to sunday around today`() {
        val wednesday = LocalDate(2026, 11, 4)

        assertTrue(isCurrentWeek(LocalDate(2026, 11, 2), wednesday))
        assertTrue(isCurrentWeek(LocalDate(2026, 11, 8), wednesday))
        assertFalse(isCurrentWeek(LocalDate(2026, 11, 1), wednesday))
        assertFalse(isCurrentWeek(LocalDate(2026, 11, 9), wednesday))
    }
}
