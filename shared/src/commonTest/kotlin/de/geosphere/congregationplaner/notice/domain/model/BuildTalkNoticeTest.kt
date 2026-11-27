package de.geosphere.congregationplaner.notice.domain.model

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BuildTalkNoticeTest {
    private val time = LocalTime(10, 0)
    private val start = LocalDate(2025, 12, 10)

    private fun slot(date: LocalDate = LocalDate(2025, 12, 14), block: (TalkSlot) -> TalkSlot = { it }) =
        block(TalkSlot(date = date, time = time))

    private fun build(slots: List<TalkSlot>, count: Int = 10, from: LocalDate = start) = buildTalkNotice(
        slots = slots,
        start = from,
        count = count,
        title = "Titel",
        congregationName = "Grafing Süd",
        printedOn = LocalDate(2026, 11, 27),
    )

    private fun firstRow(s: TalkSlot) = build(listOf(s)).rows.single()

    @Test
    fun `rows start at the start date and stop after count`() {
        val slots = listOf(7, 14, 21, 28).map { slot(LocalDate(2025, 12, it)) }
        assertEquals(listOf("So 14.12.2025", "So 21.12.2025"), build(slots, count = 2).rows.map { it.dateText })
    }

    @Test
    fun `count is clamped to one page`() {
        val base = LocalDate(2026, 1, 1).toEpochDays()
        val slots = (0 until 15).map { slot(LocalDate.fromEpochDays(base + it)) }
        assertEquals(11, build(slots, count = 20).rows.size)
    }

    @Test
    fun `a planned talk shows number title speaker with congregation and chairman`() {
        val s = slot {
            it.copy(
                talkNumber = 5,
                talkTitle = "Wie kann man als Familie glücklich sein?",
                externalSpeakerId = "e1",
                speakerFirstName = "Michael",
                speakerLastName = "Hajtud",
                speakerCongregation = "München-Harlaching",
                chairmanFirstName = "Max",
                chairmanLastName = "Muster",
            )
        }
        assertEquals(
            NoticeRow(
                "So 14.12.2025",
                "5",
                "Wie kann man als Familie glücklich sein?",
                "Michael Hajtud · München-Harlaching",
                "Max Muster",
                "",
            ),
            firstRow(s),
        )
    }

    @Test
    fun `an own member speaker shows the own congregation`() {
        val s = slot {
            it.copy(
                talkNumber = 1,
                talkTitle = "T",
                speakerMemberId = "m1",
                speakerFirstName = "Anna",
                speakerLastName = "Beispiel",
            )
        }
        assertEquals("Anna Beispiel · Grafing Süd", firstRow(s).speakerLine)
    }

    @Test
    fun `a free speaker without congregation shows only the name`() {
        val s = slot { it.copy(talkTitle = "T", speakerLastName = "Max Frei") }
        assertEquals("Max Frei", firstRow(s).speakerLine)
    }

    @Test
    fun `a talk without speaker says Redner folgt`() {
        assertEquals("Redner folgt", firstRow(slot { it.copy(talkNumber = 3, talkTitle = "T") }).speakerLine)
    }

    @Test
    fun `a week event without talk replaces the title`() {
        val row = firstRow(slot { it.copy(weekEventName = "Dienstwoche") })
        assertEquals("", row.title)
        assertEquals("Dienstwoche", row.event)
        assertEquals("", row.speakerLine)
    }

    @Test
    fun `a week event with a talk stays as event`() {
        val row = firstRow(slot { it.copy(weekEventName = "Dienstwoche", talkNumber = 2, talkTitle = "Titel Zwei") })
        assertEquals("Titel Zwei", row.title)
        assertEquals("Dienstwoche", row.event)
    }

    @Test
    fun `a cancelled talk names the event`() {
        val row = firstRow(slot { it.copy(talkCancelled = true, weekEventName = "Kongress") })
        assertEquals("Vortrag fällt aus (Kongress)", row.title)
        assertEquals("", row.event)
        assertEquals("", row.speakerLine)
        assertEquals("Vortrag fällt aus", firstRow(slot { it.copy(talkCancelled = true) }).title)
    }

    @Test
    fun `no slots from the start date gives the empty hint`() {
        val n = build(listOf(slot(LocalDate(2025, 12, 7))))
        assertEquals(emptyList(), n.rows)
        assertEquals("Keine Termine ab 10.12.2025", n.emptyHint)
    }

    @Test
    fun `footer shows the print date`() {
        val n = build(listOf(slot()))
        assertEquals("Stand: 27.11.2026", n.footer)
        assertNull(n.emptyHint)
    }

    @Test
    fun `NoticeColorSet fromKey falls back to blue`() {
        assertEquals(NoticeColorSet.BLUE, NoticeColorSet.fromKey(null))
        assertEquals(NoticeColorSet.BLUE, NoticeColorSet.fromKey("nope"))
        assertEquals(NoticeColorSet.PETROL, NoticeColorSet.fromKey("petrol"))
        assertEquals(listOf("blue", "green", "bordeaux", "anthracite", "petrol"), NoticeColorSet.entries.map { it.key })
        assertEquals(0xFF1F3864, NoticeColorSet.BLUE.primary)
        assertEquals(0xFFDCEEEF, NoticeColorSet.PETROL.stripe)
        assertEquals("Blau", NoticeColorSet.BLUE.label)
    }
}
