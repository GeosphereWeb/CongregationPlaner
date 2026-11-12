package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TalkSlotReadOnlyTest {
    private val empty = TalkSlot(LocalDate(2026, 11, 8), LocalTime(17, 30))
    private val planned = empty.copy(
        talkNumber = 12,
        talkTitle = "Titel",
        speakerMemberId = "m1",
        speakerLastName = "Muster",
        speakerFirstName = "Max",
        chairmanMemberId = "m2",
        chairmanLastName = "Beispiel",
        chairmanFirstName = "Tim",
        weekEventName = "Besuch",
        note = "Unterlagen",
    )

    @Test
    fun `an empty slot shows time and open talk and speaker`() {
        assertEquals(
            listOf("Zeit: 17:30", "Noch kein Vortrag gewählt", "Redner: Redner noch offen"),
            slotReadOnlyLines(empty),
        )
    }

    @Test
    fun `a planned slot shows every detail`() {
        assertEquals(
            listOf(
                "Ereignis: Besuch",
                "Zeit: 17:30",
                "Vortrag: Nr. 12 – Titel",
                "Redner: Muster, Max · eigene Versammlung",
                "Vorsitz: Beispiel, Tim",
                "Notiz: Unterlagen",
            ),
            slotReadOnlyLines(planned),
        )
    }

    @Test
    fun `a cancelled talk hides the speaker line`() {
        assertEquals(
            listOf("Zeit: 17:30", "Der Vortrag entfällt."),
            slotReadOnlyLines(empty.copy(talkCancelled = true)),
        )
    }

    @Test
    fun `informed is possible only for a held talk with a speaker`() {
        assertTrue(planned.canBeInformed)
        assertFalse(empty.canBeInformed)
        assertFalse(planned.copy(talkCancelled = true).canBeInformed)
        assertFalse(planned.copy(speakerMemberId = null, speakerLastName = null, speakerFirstName = null).canBeInformed)
        // Without the member id the name is a speaker entered freely, who can be informed as well.
        assertTrue(planned.copy(speakerMemberId = null).canBeInformed)
    }
}
