package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.ui.chip.StatusTone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
    fun `without a talk the slot is open`() {
        assertEquals(SlotStatus.OPEN, slotStatus(slot(), nextSlotDate = null))
    }

    @Test
    fun `a speaker without a talk is still open`() {
        val s = slot { it.copy(speakerMemberId = "m1", speakerLastName = "Muster", speakerFirstName = "Max") }

        assertEquals(SlotStatus.OPEN, slotStatus(s, nextSlotDate = null))
    }

    @Test
    fun `a talk without a speaker is missing the speaker`() {
        assertEquals(SlotStatus.SPEAKER_MISSING, slotStatus(slot(block = withTalk), nextSlotDate = null))
    }

    @Test
    fun `talk and speaker make the slot planned and confirmed makes it confirmed`() {
        assertEquals(SlotStatus.PLANNED, slotStatus(slot(block = withMember), nextSlotDate = null))
        assertEquals(
            SlotStatus.CONFIRMED,
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
        assertEquals(SlotChip("Offen", StatusTone.Coral), slotChip(s, SlotStatus.OPEN))
        assertEquals(SlotChip("Redner fehlt", StatusTone.Coral), slotChip(s, SlotStatus.SPEAKER_MISSING))
        assertEquals(SlotChip("Geplant", StatusTone.Sky), slotChip(s, SlotStatus.PLANNED))
        assertEquals(SlotChip("Bestätigt", StatusTone.Mint), slotChip(s, SlotStatus.CONFIRMED))
    }

    @Test
    fun `a cancelled slot shows the week event name`() {
        val s = slot { it.copy(weekEventId = "kongress", weekEventName = "Kongress", talkCancelled = true) }

        assertEquals(SlotChip("Kongress", StatusTone.Sky), slotChip(s, SlotStatus.CANCELLED))
    }

    @Test
    fun `title shows the talk number or that nothing is planned`() {
        assertEquals("Vortrag Nr. 12", slotTitle(slot(block = withTalk)))
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
}
