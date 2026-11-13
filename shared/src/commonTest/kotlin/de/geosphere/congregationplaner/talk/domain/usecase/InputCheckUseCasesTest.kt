package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkReferenceException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InputCheckUseCasesTest {
    private val sunday = LocalDate(2026, 11, 15)
    private val slots = listOf(TalkSlot(sunday, LocalTime(10, 0)), TalkSlot(LocalDate(2026, 11, 22), LocalTime(10, 0)))

    @Test
    fun `a day is taken by another slot but not by the slot itself`() {
        val taken = IsSlotDateTakenUseCase()
        val moved = TalkSlot(sunday, LocalTime(10, 0))

        assertTrue(taken(slots, moved, LocalDate(2026, 11, 22)))
        assertTrue(taken(slots, moved, null))
        assertFalse(taken(slots, moved, sunday))
        assertFalse(taken(slots, moved.copy(date = LocalDate(2026, 11, 29)), null))
    }

    @Test
    fun `a talk number keeps digits only up to four`() {
        assertEquals("1234", CleanTalkNumberInputUseCase()("1a2b3456789"))
        assertEquals("", CleanTalkNumberInputUseCase()("abc"))
    }

    @Test
    fun `a new talk needs a free number`() {
        val check = CheckNewTalkNumberUseCase()
        val talks = listOf(Talk(1, "Eins"))

        assertIs<TalkReferenceException.MissingNumber>(check(null, talks))
        assertEquals("Die Nummer 1 gibt es schon.", check(1, talks)?.message)
        assertNull(check(2, talks))
    }
}
