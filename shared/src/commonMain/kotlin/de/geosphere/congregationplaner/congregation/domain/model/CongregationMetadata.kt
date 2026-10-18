package de.geosphere.congregationplaner.congregation.domain.model

import kotlinx.datetime.LocalTime

data class CongregationMetadata(
    val id: String = "",
    val name: String = "",
    val midweekMeetingTime: LocalTime = LocalTime(19, 0),
    val midweekMeetingWeekday: Int = 3,
    val weekendMeetingTime: LocalTime = LocalTime(17, 30),
    val weekendMeetingWeekday: Int = 7,
)
