package de.geosphere.congregationplaner.importer

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExcelDatesTest {
    @Test
    fun knownSerialConvertsToTheRightDate() {
        assertEquals(LocalDate(2023, 1, 1), excelSerialToDate(44927.0))
        assertEquals(LocalDate(2027, 12, 26), excelSerialToDate(46747.0))
    }

    @Test
    fun timeOfDayFractionIsIgnored() {
        assertEquals(LocalDate(2023, 1, 1), excelSerialToDate(44927.75))
    }

    @Test
    fun datesBeforeTheFakeLeapDayCountFromOneJanuary1900() {
        assertEquals(LocalDate(1900, 1, 1), excelSerialToDate(1.0))
        assertEquals(LocalDate(1900, 2, 28), excelSerialToDate(59.0))
    }

    @Test
    fun dayAfterTheFakeLeapDayIsFirstOfMarch1900() {
        assertEquals(LocalDate(1900, 3, 1), excelSerialToDate(61.0))
    }

    @Test
    fun theNonExistentLeapDayOf1900IsRejected() {
        assertFailsWith<IllegalArgumentException> { excelSerialToDate(60.0) }
    }

    @Test
    fun zeroAndNegativeValuesAreRejected() {
        assertFailsWith<IllegalArgumentException> { excelSerialToDate(0.0) }
        assertFailsWith<IllegalArgumentException> { excelSerialToDate(-5.0) }
    }

    @Test
    fun consecutiveSerialsAreConsecutiveDaysAcrossALeapYear() {
        assertEquals(LocalDate(2024, 2, 29), excelSerialToDate(45351.0))
        assertEquals(LocalDate(2024, 3, 1), excelSerialToDate(45352.0))
    }
}
