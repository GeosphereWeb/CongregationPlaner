package de.geosphere.congregationplaner.congregation.presentation

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import kotlinx.datetime.LocalTime

internal val previewCongregations = listOf(
    CongregationMetadata(
        id = "berlin-mitte",
        name = "Berlin-Mitte",
        midweekMeetingTime = LocalTime(19, 0),
        midweekMeetingWeekday = 3,
        weekendMeetingTime = LocalTime(10, 0),
        weekendMeetingWeekday = 7,
    ),
    CongregationMetadata(
        id = "berlin-nord",
        name = "Berlin-Nord",
        midweekMeetingTime = LocalTime(18, 30),
        midweekMeetingWeekday = 4,
        weekendMeetingTime = LocalTime(16, 0),
        weekendMeetingWeekday = 6,
    ),
)
