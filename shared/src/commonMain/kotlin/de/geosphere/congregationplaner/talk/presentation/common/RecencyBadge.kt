package de.geosphere.congregationplaner.talk.presentation.common

import de.geosphere.congregationplaner.talk.domain.model.RecencyLevel
import de.geosphere.congregationplaner.talk.domain.model.recencyLevelOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.monthsUntil
import kotlinx.datetime.yearsUntil

/** Stufe und Text des Chips „wie lange her“, z. B. „vor 5 J. · 2×“ (CP-191). */
data class RecencyBadge(val level: RecencyLevel, val label: String)

private const val DAYS_PER_WEEK = 7

/**
 * Der Chip zum letzten Tag [lastHeld] und der Zahl der Tage [count] aus Sicht von [today]: unter 7 Tagen „diese
 * Woche“, unter einem Monat in Wochen, unter einem Jahr in Monaten, sonst in Jahren; ein späterer Tag ist „geplant“,
 * ohne Tag „nie“.
 */
fun recencyBadgeOf(lastHeld: LocalDate?, count: Int, today: LocalDate): RecencyBadge {
    val level = recencyLevelOf(lastHeld, today)
    if (lastHeld == null) return RecencyBadge(level, "nie")
    val days = lastHeld.daysUntil(today)
    val months = lastHeld.monthsUntil(today)
    val years = lastHeld.yearsUntil(today)
    val distance = when {
        days < 0 -> "geplant"
        days < DAYS_PER_WEEK -> "diese Woche"
        months < 1 -> "vor ${days / DAYS_PER_WEEK} Wo."
        years < 1 -> "vor $months Mon."
        else -> "vor $years J."
    }
    return RecencyBadge(level, "$distance · $count×")
}
