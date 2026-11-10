package de.geosphere.congregationplaner.importer.domain.planning

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

private const val FIRST_SERIAL_AFTER_FAKE_LEAP_DAY = 61
private const val FAKE_LEAP_DAY_SERIAL = 60

// Excel's 1900 date system counts 1900-01-01 as day 1 and wrongly treats 1900 as a leap year (day 60 = "1900-02-29").
private val EPOCH_FROM_MARCH_1900 = LocalDate(1899, 12, 30)
private val EPOCH_BEFORE_MARCH_1900 = LocalDate(1899, 12, 31)

/**
 * Converts an Excel serial date (1900 date system, e.g. `44927` = 2023-01-01) to a date. A time-of-day fraction is
 * ignored. Throws [IllegalArgumentException] for values that are no real date (below 1 or the non-existent 1900-02-29).
 */
fun excelSerialToDate(serial: Double): LocalDate {
    val day = serial.toInt()
    require(day >= 1 && day != FAKE_LEAP_DAY_SERIAL) { "Keine gültige Excel-Datumsnummer: $serial" }
    val epoch = if (day >= FIRST_SERIAL_AFTER_FAKE_LEAP_DAY) EPOCH_FROM_MARCH_1900 else EPOCH_BEFORE_MARCH_1900
    return epoch.plus(day, DateTimeUnit.DAY)
}
