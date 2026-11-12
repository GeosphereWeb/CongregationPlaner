package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ReassignTextsTest {
    private val source = TalkSlot(
        LocalDate(2026, 11, 8),
        LocalTime(17, 30),
        talkNumber = 12,
        talkTitle = "Der Weg zum Glück",
        speakerMemberId = "m1",
        speakerLastName = "Muster",
        speakerFirstName = "Max",
    )
    private val target = TalkSlot(LocalDate(2026, 11, 15), LocalTime(17, 30), talkTitle = "Sondervortrag")

    @Test
    fun `texts name talk speaker and both days`() {
        assertEquals("Nr. 12 – Der Weg zum Glück · Muster, Max", slotAssignmentText(source))
        assertEquals(
            "„Nr. 12 – Der Weg zum Glück · Muster, Max“ vom So, 08.11.2026 umplanen: Zieltag in der Liste antippen.",
            reassignBannerText(source),
        )
        assertEquals(
            "Am So, 15.11.2026 ist schon „Sondervortrag (frei eingetragen)“ eingeplant. " +
                "Sollen die Termine So, 08.11.2026 und So, 15.11.2026 tauschen?",
            swapQuestion(source, target),
        )
        assertEquals("Umgeplant: So, 08.11.2026 → So, 15.11.2026.", reassignedMessage(source, target))
    }
}
