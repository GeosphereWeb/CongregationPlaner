package de.geosphere.congregationplaner.talk.presentation

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTextTest {
    @Test
    fun displayTextHasWeekdayAndPaddedDayAndMonth() {
        assertEquals("So, 25.10.2026", LocalDate(2026, 10, 25).toDisplayText())
        assertEquals("Mo, 05.01.2026", LocalDate(2026, 1, 5).toDisplayText())
    }

    @Test
    fun pickerMillisRoundTripKeepsTheDate() {
        val date = LocalDate(2026, 12, 31)
        assertEquals(date, dateFromPickerMillis(date.toPickerMillis()))
    }

    @Test
    fun pickerMillisAreUtcMidnight() {
        assertEquals(0L, LocalDate(1970, 1, 1).toPickerMillis())
    }
}
