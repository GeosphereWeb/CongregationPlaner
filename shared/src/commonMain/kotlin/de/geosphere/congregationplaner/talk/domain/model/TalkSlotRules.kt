package de.geosphere.congregationplaner.talk.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

/** Die Termine ab [today], an denen ein Vortrag gehalten wird (abgesagte zählen nicht), nach Datum sortiert. */
fun List<TalkSlot>.upcomingTalkSlots(today: LocalDate): List<TalkSlot> =
    distinctBy { it.date }.filter { it.date >= today && !it.talkCancelled }.sortedBy { it.date }

/** Alle Tage des Jahres [year] am Wochentag [weekday] (1 = Montag … 7 = Sonntag), aufsteigend. */
fun datesOfWeekdayIn(year: Int, weekday: Int): List<LocalDate> {
    require(weekday in 1..7) { "Der Wochentag muss zwischen 1 und 7 liegen." }
    var first = LocalDate(year, 1, 1)
    while (first.dayOfWeek.isoDayNumber != weekday) first = first.plus(1, DateTimeUnit.DAY)
    return generateSequence(first) { it.plus(7, DateTimeUnit.DAY) }.takeWhile { it.year == year }.toList()
}
