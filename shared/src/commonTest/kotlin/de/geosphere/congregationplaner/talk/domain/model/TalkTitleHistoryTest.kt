package de.geosphere.congregationplaner.talk.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TalkTitleHistoryTest {
    private val talk = Talk(
        12,
        "Erster Titel",
        titleChanges = listOf(
            TalkTitleChange(LocalDate(2028, 1, 1), "Dritter Titel"),
            TalkTitleChange(LocalDate(2027, 1, 1), "Zweiter Titel"),
        ),
    )

    @Test
    fun `the title valid on a day follows the changes in date order`() {
        assertEquals("Erster Titel", talk.titleOn(LocalDate(2026, 12, 31)))
        assertEquals("Zweiter Titel", talk.titleOn(LocalDate(2027, 1, 1)))
        assertEquals("Zweiter Titel", talk.titleOn(LocalDate(2027, 12, 31)))
        assertEquals("Dritter Titel", talk.titleOn(LocalDate(2030, 6, 1)))
    }

    @Test
    fun `asOf keeps number and changes and only swaps the title`() {
        val asOf = listOf(talk).asOf(LocalDate(2027, 3, 1)).single()

        assertEquals("Zweiter Titel", asOf.title)
        assertEquals(talk.titleChanges, asOf.titleChanges)
        assertEquals(12, asOf.number)
    }
}
