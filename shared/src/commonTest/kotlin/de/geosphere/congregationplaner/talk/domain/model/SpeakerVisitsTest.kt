package de.geosphere.congregationplaner.talk.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class SpeakerVisitsTest {
    private fun slot(day: LocalDate, external: String? = null, member: String? = null) = TalkSlot(
        date = day,
        time = LocalTime(10, 0),
        talkNumber = 12,
        speakerMemberId = member,
        speakerLastName = "Name",
        externalSpeakerId = external,
    )

    @Test
    fun `visits count distinct days and keep the latest`() {
        val visits = speakerVisitsOf(
            listOf(
                slot(LocalDate(2024, 3, 3), external = "e1"),
                slot(LocalDate(2026, 1, 4), external = "e1"),
                slot(LocalDate(2026, 1, 4), external = "e1").copy(time = LocalTime(16, 0)),
            ),
        )

        assertEquals(mapOf("e1" to SpeakerVisits(LocalDate(2026, 1, 4), 2)), visits)
    }

    @Test
    fun `cancelled talks and slots without external speaker do not count`() {
        val visits = speakerVisitsOf(
            listOf(
                slot(LocalDate(2025, 5, 4), external = "e2").copy(talkCancelled = true),
                slot(LocalDate(2025, 6, 1), member = "m1"),
            ),
        )

        assertEquals(emptyMap(), visits)
    }
}
