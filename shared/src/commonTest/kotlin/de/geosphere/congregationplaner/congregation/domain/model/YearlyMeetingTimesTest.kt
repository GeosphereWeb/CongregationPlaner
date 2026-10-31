package de.geosphere.congregationplaner.congregation.domain.model

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class YearlyMeetingTimesTest {
    private val next = MeetingTimes(4, LocalTime(18, 30), 6, LocalTime(9, 30))

    @Test
    fun yearlyTimesSurviveEncodingAndDecoding() {
        val encoded = mapOf(2027 to next).encodeYearlyMeetingTimes()

        assertEquals(listOf("2027|4|18:30|6|09:30"), encoded)
        assertEquals(mapOf(2027 to next), encoded.decodeYearlyMeetingTimes())
    }

    @Test
    fun malformedEntriesAreSkipped() {
        val decoded = listOf("garbage", null, "2027|9|18:30|6|09:30", "2028|4|18:30|6|09:30")
            .decodeYearlyMeetingTimes()

        assertEquals(setOf(2028), decoded.keys)
    }

    @Test
    fun yearWithoutOverrideFallsBackToDefaultTimes() {
        val congregation = CongregationMetadata(yearlyMeetingTimes = mapOf(2027 to next))

        assertEquals(next, congregation.meetingTimesFor(2027))
        assertEquals(MeetingTimes(), congregation.meetingTimesFor(2026))
    }
}
