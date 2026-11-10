package de.geosphere.congregationplaner.talk.presentation.schedule

import androidx.compose.ui.text.font.FontWeight
import de.geosphere.congregationplaner.presentation.components.CardHighlight
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.Speaker
import de.geosphere.congregationplaner.talk.domain.model.SpeakerContact
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

        assertEquals(SlotChip("Nächster", StatusTone.Gold), slotChip(SlotStatus.NEXT))
        assertEquals(SlotChip("Entfällt", StatusTone.Sky), slotChip(SlotStatus.CANCELLED))
        assertEquals(SlotChip("Nicht geplant", StatusTone.Coral), slotChip(SlotStatus.NOT_PLANNED))
        assertEquals(SlotChip("Geplant", StatusTone.Sky), slotChip(SlotStatus.PLANNED))
        assertEquals(SlotChip("Informiert", StatusTone.Mint), slotChip(SlotStatus.INFORMED))
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
    fun `the week event name is its own line also for a cancelled slot`() {
        val withEvent = slot { it.copy(weekEventId = "besuch", weekEventName = "Dienstwoche") }
        val cancelled = slot { it.copy(weekEventId = "kongress", weekEventName = "Kongress", talkCancelled = true) }

        assertEquals("Dienstwoche", slotEventLine(withEvent))
        assertEquals("Kongress", slotEventLine(cancelled))
        assertNull(slotEventLine(slot()))
    }

    @Test
    fun `a cancelled slot keeps the cancelled chip and has the week event name only in the event line`() {
        val s = slot { it.copy(weekEventId = "kongress", weekEventName = "Kongress", talkCancelled = true) }

        assertEquals(SlotChip("Entfällt", StatusTone.Sky), listSlotChip(s, SlotStatus.CANCELLED))
        assertEquals("Kongress", slotEventLine(s))
    }

    @Test
    fun `a cancelled slot shows neither title nor speaker in the list`() {
        val cancelled = slot { it.copy(weekEventId = "kongress", weekEventName = "Kongress", talkCancelled = true) }
        val open = slot()

        assertNull(slotListTitle(cancelled))
        assertNull(slotListSpeakerLine(cancelled))
        assertEquals("Noch nicht geplant", slotListTitle(open)?.text)
        assertEquals("Redner noch offen", slotListSpeakerLine(open))
    }

    @Test
    fun `the list title shows the talk number in bold`() {
        val title = slotListTitle(slot(block = withTalk))!!

        assertEquals("Nr. 12 – Der Weg zum Glück", title.text)
        assertEquals(1, title.spanStyles.size)
        assertEquals(FontWeight.Bold, title.spanStyles.single().item.fontWeight)
        assertEquals("Nr. 12", title.text.substring(title.spanStyles.single().start, title.spanStyles.single().end))
        assertEquals("Nr. 12", slotListTitle(slot { it.copy(talkNumber = 12, talkTitle = null) })!!.text)
    }

    @Test
    fun `title shows the talk number and topic or that nothing is planned`() {
        assertEquals("Nr. 12 – Der Weg zum Glück", slotTitle(slot(block = withTalk)))
        assertEquals("Nr. 12", slotTitle(slot { it.copy(talkNumber = 12, talkTitle = null) }))
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
        assertEquals("Extern, Eva · Nord", slotSpeakerLine(external))
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

    @Test
    fun `highlight marks the current week blue and upcoming unplanned slots red`() {
        val wednesday = LocalDate(2026, 11, 4)
        val thisWeek = slot(LocalDate(2026, 11, 8))
        val laterUnplanned = slot(LocalDate(2026, 11, 15))
        val laterPlanned = slot(LocalDate(2026, 11, 22), withMember)
        val pastUnplanned = slot(LocalDate(2026, 10, 25))
        val laterCancelled = slot(LocalDate(2026, 11, 29)) { it.copy(talkCancelled = true) }

        // The current week wins even when the slot is not planned yet.
        assertEquals(CardHighlight.Info, slotHighlight(thisWeek, wednesday))
        assertEquals(CardHighlight.Warning, slotHighlight(laterUnplanned, wednesday))
        assertEquals(CardHighlight.None, slotHighlight(laterPlanned, wednesday))
        assertEquals(CardHighlight.None, slotHighlight(pastUnplanned, wednesday))
        assertEquals(CardHighlight.None, slotHighlight(laterCancelled, wednesday))
    }

    @Test
    fun `the list shows no chip for a planned or unplanned slot but keeps the others`() {
        val s = slot()

        assertNull(listSlotChip(s, SlotStatus.PLANNED))
        assertEquals(SlotChip("Informiert", StatusTone.Mint), listSlotChip(s, SlotStatus.INFORMED))
        assertNull(listSlotChip(s, SlotStatus.NOT_PLANNED))
        assertEquals(SlotChip("Nächster", StatusTone.Gold), listSlotChip(s, SlotStatus.NEXT))
    }

    @Test
    fun `common time is the most frequent one and the earliest on a tie`() {
        val early = LocalTime(9, 30)
        assertEquals(time, commonSlotTime(listOf(slot(), slot(), slot { it.copy(time = early) })))
        assertEquals(early, commonSlotTime(listOf(slot(), slot { it.copy(time = early) })))
        assertNull(commonSlotTime(emptyList()))
    }

    @Test
    fun `the contact of a member speaker comes from the member list`() {
        val member = slot { it.copy(speakerMemberId = "m1", speakerLastName = "Muster", speakerFirstName = "Max") }
        val speakers = listOf(
            Speaker("m1", "Muster", "Max", mobilePhone = "0170 1234567"),
            Speaker("m2", "Berg", "Lea"),
        )

        assertEquals(SpeakerContact(mobilePhone = "0170 1234567"), slotContact(member, speakers, emptyList()))
        assertNull(slotContact(member, emptyList(), emptyList()))
        assertNull(slotContact(member, listOf(Speaker("m1", "Muster", "Max")), emptyList()))
    }

    @Test
    fun `the contact of an external speaker comes from the directory`() {
        val external = slot {
            it.copy(
                externalSpeakerId = "e1",
                speakerLastName = "Extern",
                speakerFirstName = "Eva",
                speakerCongregation = "Nord",
            )
        }
        val directory = listOf(ExternalSpeaker("e1", "Extern", "Eva", "Nord", landlinePhone = "089 123456"))

        assertEquals(SpeakerContact(landlinePhone = "089 123456"), slotContact(external, emptyList(), directory))
        assertNull(slotContact(external, emptyList(), emptyList()))
    }

    @Test
    fun `a slot without a speaker has no contact`() {
        val speakers = listOf(Speaker("m1", "Muster", "Max", mobilePhone = "0170 1234567"))

        assertNull(slotContact(slot(), speakers, emptyList()))
    }
}
