package de.geosphere.congregationplaner.home.presentation

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeTextsTest {
    private val member = TalkSlot(
        LocalDate(2026, 11, 8),
        LocalTime(17, 30),
        talkNumber = 12,
        talkTitle = "Der Weg zum Glück",
        speakerMemberId = "m1",
        speakerLastName = "Muster",
        speakerFirstName = "Max",
    )

    @Test
    fun `next talk shows the day and then talk and speaker`() {
        assertEquals("Sonntag, 8. November", nextTalkTitle(member))
        assertEquals(
            "Vortrag Nr. 12 – Der Weg zum Glück\nMuster, Max · eigene Versammlung",
            nextTalkDetails(member),
        )
    }

    @Test
    fun `next talk without plan says so`() {
        val empty = TalkSlot(LocalDate(2026, 11, 8), LocalTime(17, 30))

        assertEquals("Noch nicht geplant\nRedner noch offen", nextTalkDetails(empty))
    }

    @Test
    fun `open weekends are counted in the right form`() {
        assertEquals("Alles geplant", openWeekendsTitle(0))
        assertEquals("1 Wochenende noch offen", openWeekendsTitle(1))
        assertEquals("3 Wochenenden noch offen", openWeekendsTitle(3))
    }

    @Test
    fun `the next gap is named with its day`() {
        val dates = listOf(LocalDate(2026, 11, 15), LocalDate(2026, 11, 22))

        assertEquals("Nächste Lücke: So, 15. November", openWeekendsDetails(dates))
        assertNull(openWeekendsDetails(emptyList()))
    }

    @Test
    fun `approvals are counted in the right form`() {
        assertEquals("Keine offenen Freigaben", approvalsTitle(0))
        assertEquals("1 Freigabe offen", approvalsTitle(1))
        assertEquals("2 Freigaben offen", approvalsTitle(2))
    }

    @Test
    fun `the badge appears only for open approvals`() {
        assertEquals("2 offen", approvalsBadge(2))
        assertNull(approvalsBadge(0))
        assertNull(approvalsBadge(null))
    }
}
