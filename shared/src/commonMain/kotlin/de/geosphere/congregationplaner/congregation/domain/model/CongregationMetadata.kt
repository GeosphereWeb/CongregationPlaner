package de.geosphere.congregationplaner.congregation.domain.model

import kotlinx.datetime.LocalTime

/** Wochentage (1 = Montag … 7 = Sonntag) und Uhrzeiten der beiden wöchentlichen Zusammenkünfte. */
data class MeetingTimes(
    val midweekWeekday: Int = 3,
    val midweekTime: LocalTime = LocalTime(19, 0),
    val weekendWeekday: Int = 7,
    val weekendTime: LocalTime = LocalTime(17, 30),
)

data class CongregationMetadata(
    val id: String = "",
    val name: String = "",
    val midweekMeetingTime: LocalTime = LocalTime(19, 0),
    val midweekMeetingWeekday: Int = 3,
    val weekendMeetingTime: LocalTime = LocalTime(17, 30),
    val weekendMeetingWeekday: Int = 7,
    /** Abweichende Zeiten je Kalenderjahr; Jahre ohne Eintrag nutzen die Standardzeiten oben. */
    val yearlyMeetingTimes: Map<Int, MeetingTimes> = emptyMap(),
) {
    val defaultMeetingTimes: MeetingTimes
        get() = MeetingTimes(midweekMeetingWeekday, midweekMeetingTime, weekendMeetingWeekday, weekendMeetingTime)

    fun meetingTimesFor(year: Int): MeetingTimes = yearlyMeetingTimes[year] ?: defaultMeetingTimes
}

/** Speicherformat eines Jahreseintrags: `jahr|wochentagMitte|zeitMitte|wochentagWE|zeitWE`, z. B. `2027|4|18:30|6|09:30`. */
fun Map<Int, MeetingTimes>.encodeYearlyMeetingTimes(): List<String> = entries.sortedBy { it.key }.map { (year, t) ->
    "$year|${t.midweekWeekday}|${t.midweekTime}|${t.weekendWeekday}|${t.weekendTime}"
}

/** Fehlerhafte Einträge werden übersprungen, damit ein beschädigter Wert nicht die ganze Versammlung unlesbar macht. */
fun List<String?>.decodeYearlyMeetingTimes(): Map<Int, MeetingTimes> = mapNotNull { entry ->
    runCatching {
        val parts = entry!!.split('|')
        require(parts.size == 5 && parts[1].toInt() in 1..7 && parts[3].toInt() in 1..7)
        parts[0].toInt() to MeetingTimes(
            midweekWeekday = parts[1].toInt(),
            midweekTime = LocalTime.parse(parts[2]),
            weekendWeekday = parts[3].toInt(),
            weekendTime = LocalTime.parse(parts[4]),
        )
    }.getOrNull()
}.toMap()
