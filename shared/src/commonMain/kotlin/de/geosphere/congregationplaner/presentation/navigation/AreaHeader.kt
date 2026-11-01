package de.geosphere.congregationplaner.presentation.navigation

import de.geosphere.congregationplaner.organization.domain.model.SelectedCongregation
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

/** Texte des Kopfbereichs einer Übersicht (siehe `TeleprompterHeader`). */
internal data class AreaHeader(val stepLabel: String, val previous: List<String>, val current: String)

private val WEEKDAYS = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")

private val MONTHS = listOf(
    "Januar",
    "Februar",
    "März",
    "April",
    "Mai",
    "Juni",
    "Juli",
    "August",
    "September",
    "Oktober",
    "November",
    "Dezember",
)

internal fun germanWeekdayName(date: LocalDate): String = WEEKDAYS[date.dayOfWeek.isoDayNumber - 1]

internal fun germanMonthName(date: LocalDate): String = MONTHS[date.month.ordinal]

/** „Sonntag, 1. November“ */
internal fun germanDayAndMonth(date: LocalDate): String =
    "${germanWeekdayName(date)}, ${date.day}. ${germanMonthName(date)}"

/** Globale Admins haben keine Versammlung ([congregation] ist `null`, ADR-0013); die Zeile bleibt dann leer. */
internal fun homeHeader(congregation: SelectedCongregation?, today: LocalDate) = AreaHeader(
    stepLabel = congregation?.displayName?.uppercase().orEmpty(),
    previous = listOf(germanDayAndMonth(today)),
    current = "Diese Woche",
)

internal fun congregationHeader(congregation: SelectedCongregation?) = AreaHeader(
    stepLabel = congregation?.let { "KREIS ${it.ref.circuitId}" }.orEmpty(),
    previous = listOf(TopLevelDestination.CONGREGATION.label),
    current = congregation?.displayName.orEmpty(),
)

internal fun globalHeader() = AreaHeader(
    stepLabel = "GLOBAL",
    previous = emptyList(),
    current = TopLevelDestination.GLOBAL_ADMIN.label,
)
