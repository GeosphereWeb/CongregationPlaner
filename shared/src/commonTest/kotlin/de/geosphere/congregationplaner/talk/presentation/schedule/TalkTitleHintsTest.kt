package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.TalkTitleChange
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TalkTitleHintsTest {
    private val talks = listOf(
        Talk(12, "Alt", titleChanges = listOf(TalkTitleChange(LocalDate(2027, 1, 1), "Neu"))),
        Talk(40, "Pausiert", active = false),
    )

    private fun slot(date: LocalDate, number: Int?, title: String?) =
        TalkSlot(date, LocalTime(10, 0), talkNumber = number, talkTitle = title)

    @Test
    fun `a future slot planned with the old title gets the new one offered`() {
        assertEquals("Neu", outdatedTalkTitle(slot(LocalDate(2027, 2, 7), 12, "Alt"), talks))
    }

    @Test
    fun `held slots and slots with the valid title need no update`() {
        assertNull(outdatedTalkTitle(slot(LocalDate(2026, 11, 15), 12, "Alt"), talks))
        assertNull(outdatedTalkTitle(slot(LocalDate(2027, 2, 7), 12, "Neu"), talks))
        assertNull(outdatedTalkTitle(slot(LocalDate(2027, 2, 7), null, "Frei"), talks))
        assertNull(outdatedTalkTitle(slot(LocalDate(2027, 2, 7), 99, "Unbekannt"), talks))
    }

    @Test
    fun `an inactive talk gets a hint`() {
        assertEquals(
            "Nr. 40 ist in der Vortragsliste nicht mehr aktiv; bitte prüfen, ob er noch gehalten wird.",
            inactiveTalkHint(slot(LocalDate(2027, 2, 7), 40, "Pausiert"), talks),
        )
        assertNull(inactiveTalkHint(slot(LocalDate(2027, 2, 7), 12, "Neu"), talks))
    }
}
