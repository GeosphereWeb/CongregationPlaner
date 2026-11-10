package de.geosphere.congregationplaner.talk.presentation.common

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val WEEKDAY_SHORT_NAMES = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

/** Deutsche Anzeige mit Wochentag, z. B. `So, 25.10.2026`. */
internal fun LocalDate.toDisplayText(): String {
    val dayText = day.toString().padStart(2, '0')
    val monthText = month.number.toString().padStart(2, '0')
    return "${WEEKDAY_SHORT_NAMES[dayOfWeek.isoDayNumber - 1]}, $dayText.$monthText.$year"
}

/** Der Datumswähler rechnet in UTC-Mitternacht, damit die Zeitzone das Datum nicht verschiebt. */
internal fun LocalDate.toPickerMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

internal fun dateFromPickerMillis(millis: Long): LocalDate =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
