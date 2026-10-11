package de.geosphere.congregationplaner.congregation.domain.model

data class CongregationMetadata(
    val id: String = "",
    val name: String = "",
    val midweekMeetingTime: String = "",
    val midweekMeetingWeekday: String = "",
    val weekendMeetingTime: String = "",
    val weekendMeetingWeekday: String = "",
)
