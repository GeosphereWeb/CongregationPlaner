package de.geosphere.congregationplaner.congregation.presentation

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class CongregationFormattingTest {
    @Test
    fun `weekday names cover monday to sunday and invalid values`() {
        assertEquals(
            listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"),
            (1..7).map(::weekdayName),
        )
        assertEquals("Ungültig", weekdayName(0))
        assertEquals("Ungültig", weekdayName(8))
    }

    @Test
    fun `meeting time is formatted with two digit hour and minute`() {
        assertEquals("09:05", LocalTime(9, 5).toInputValue())
        assertEquals("19:30", LocalTime(19, 30).toInputValue())
    }
}
