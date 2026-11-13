package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.TalkTitleChange
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CheckSlotTalkUseCaseTest {
    private val check = CheckSlotTalkUseCase()
    private val talks = listOf(
        Talk(12, "Alt", titleChanges = listOf(TalkTitleChange(LocalDate(2027, 1, 1), "Neu"))),
        Talk(40, "Pausiert", active = false),
    )

    private fun slot(date: LocalDate, number: Int?, title: String?) =
        TalkSlot(date, LocalTime(10, 0), talkNumber = number, talkTitle = title)

    @Test
    fun `a future slot planned with the old title gets the new one offered`() {
        assertEquals("Neu", check(slot(LocalDate(2027, 2, 7), 12, "Alt"), talks).outdatedTitle)
    }

    @Test
    fun `held slots and slots with the valid title need no update`() {
        assertNull(check(slot(LocalDate(2026, 11, 15), 12, "Alt"), talks).outdatedTitle)
        assertNull(check(slot(LocalDate(2027, 2, 7), 12, "Neu"), talks).outdatedTitle)
        assertNull(check(slot(LocalDate(2027, 2, 7), null, "Frei"), talks).outdatedTitle)
        assertNull(check(slot(LocalDate(2027, 2, 7), 99, "Unbekannt"), talks).outdatedTitle)
    }

    @Test
    fun `an inactive talk is reported and the list carries the titles of the day`() {
        val result = check(slot(LocalDate(2027, 2, 7), 40, "Pausiert"), talks)

        assertEquals(40, result.inactiveNumber)
        assertEquals(listOf("Neu", "Pausiert"), result.talksOnDate.map { it.title })
        assertNull(check(slot(LocalDate(2027, 2, 7), 12, "Neu"), talks).inactiveNumber)
    }
}
