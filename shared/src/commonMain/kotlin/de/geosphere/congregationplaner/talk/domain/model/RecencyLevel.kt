package de.geosphere.congregationplaner.talk.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.yearsUntil

/** Wie lange ein Vortrag zuletzt her ist, in den Stufen der Farbskala (CP-191). */
enum class RecencyLevel {
    /** Unter einem Jahr oder schon für einen späteren Tag geplant. */
    RECENT,
    ONE_TO_TWO_YEARS,
    TWO_TO_FOUR_YEARS,
    FOUR_TO_SIX_YEARS,
    OVER_SIX_YEARS,

    /** Noch nie gehalten. */
    NEVER,
}

private const val TWO_YEARS = 2
private const val FOUR_YEARS = 4
private const val SIX_YEARS = 6

/**
 * Die Stufe zum letzten Tag [lastHeld] aus Sicht von [today]; gezählt werden volle Jahre, genau auf einer Grenze gilt
 * die ältere Stufe (genau 2 Jahre → [RecencyLevel.TWO_TO_FOUR_YEARS]). Ein Tag nach [today] ist geplant und zählt als
 * [RecencyLevel.RECENT].
 */
fun recencyLevelOf(lastHeld: LocalDate?, today: LocalDate): RecencyLevel {
    if (lastHeld == null) return RecencyLevel.NEVER
    if (lastHeld > today) return RecencyLevel.RECENT
    val years = lastHeld.yearsUntil(today)
    return when {
        years < 1 -> RecencyLevel.RECENT
        years < TWO_YEARS -> RecencyLevel.ONE_TO_TWO_YEARS
        years < FOUR_YEARS -> RecencyLevel.TWO_TO_FOUR_YEARS
        years < SIX_YEARS -> RecencyLevel.FOUR_TO_SIX_YEARS
        else -> RecencyLevel.OVER_SIX_YEARS
    }
}
