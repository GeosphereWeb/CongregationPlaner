package de.geosphere.congregationplaner.congregation.domain.model

import kotlinx.datetime.LocalTime

private const val DEFAULT_MIDWEEK_WEEKDAY = 3
private const val DEFAULT_WEEKEND_WEEKDAY = 7
private val DEFAULT_MIDWEEK_TIME = LocalTime(19, 0)
private val DEFAULT_WEEKEND_TIME = LocalTime(17, 30)
private const val ENCODED_PART_COUNT = 5
private val WEEKDAYS = 1..DEFAULT_WEEKEND_WEEKDAY

/** Wochentage (1 = Montag … 7 = Sonntag) und Uhrzeiten der beiden wöchentlichen Zusammenkünfte. */
data class MeetingTimes(
    val midweekWeekday: Int = DEFAULT_MIDWEEK_WEEKDAY,
    val midweekTime: LocalTime = DEFAULT_MIDWEEK_TIME,
    val weekendWeekday: Int = DEFAULT_WEEKEND_WEEKDAY,
    val weekendTime: LocalTime = DEFAULT_WEEKEND_TIME,
)

data class CongregationMetadata(
    val id: String = "",
    val name: String = "",
    val midweekMeetingTime: LocalTime = DEFAULT_MIDWEEK_TIME,
    val midweekMeetingWeekday: Int = DEFAULT_MIDWEEK_WEEKDAY,
    val weekendMeetingTime: LocalTime = DEFAULT_WEEKEND_TIME,
    val weekendMeetingWeekday: Int = DEFAULT_WEEKEND_WEEKDAY,
    /** Abweichende Zeiten je Kalenderjahr; Jahre ohne Eintrag nutzen die Standardzeiten oben. */
    val yearlyMeetingTimes: Map<Int, MeetingTimes> = emptyMap(),
) {
    val defaultMeetingTimes: MeetingTimes
        get() = MeetingTimes(midweekMeetingWeekday, midweekMeetingTime, weekendMeetingWeekday, weekendMeetingTime)

    fun meetingTimesFor(year: Int): MeetingTimes = yearlyMeetingTimes[year] ?: defaultMeetingTimes
}

/**
 * Speicherformat eines Jahreseintrags: `jahr|wochentagMitte|zeitMitte|wochentagWE|zeitWE`, z. B.
 * `2027|4|18:30|6|09:30`.
 */
fun Map<Int, MeetingTimes>.encodeYearlyMeetingTimes(): List<String> = entries.sortedBy { it.key }.map { (year, t) ->
    "$year|${t.midweekWeekday}|${t.midweekTime}|${t.weekendWeekday}|${t.weekendTime}"
}

/** Fehlerhafte Einträge werden übersprungen, damit ein beschädigter Wert nicht die ganze Versammlung unlesbar macht. */
fun List<String?>.decodeYearlyMeetingTimes(): Map<Int, MeetingTimes> = mapNotNull { entry ->
    runCatching {
        val parts = entry!!.split('|')
        require(parts.size == ENCODED_PART_COUNT)
        val part = parts.iterator()
        part.next().toInt() to MeetingTimes(
            midweekWeekday = part.next().toInt(),
            midweekTime = LocalTime.parse(part.next()),
            weekendWeekday = part.next().toInt(),
            weekendTime = LocalTime.parse(part.next()),
        ).also { require(it.midweekWeekday in WEEKDAYS && it.weekendWeekday in WEEKDAYS) }
    }.getOrNull()
}.toMap()
