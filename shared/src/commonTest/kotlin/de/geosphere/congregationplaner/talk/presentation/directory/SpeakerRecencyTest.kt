package de.geosphere.congregationplaner.talk.presentation.directory

import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.SpeakerVisits
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpeakerRecencyTest {
    private val today = LocalDate(2026, 11, 25)
    private val altdorf = ExternalCongregation("g1", "c1", "Altdorf")
    private val berg = VisitingSpeaker("s1", "g1", "Berg", "Lea")
    private val adam = VisitingSpeaker("s2", "g1", "Adam", "Tom")
    private val cron = VisitingSpeaker("s3", "g1", "Cron", "Max")
    private val dahl = VisitingSpeaker("s4", "g1", "Dahl", "Ute")

    private fun state(visits: Map<String, SpeakerVisits>?) = ExternalDirectoryUiState(
        directory = ExternalDirectory(congregations = listOf(altdorf), speakers = listOf(berg, adam, cron, dahl)),
        visits = visits,
    )

    @Test
    fun `speakers without visit come first then the oldest visit`() {
        val visits = mapOf(
            "s1" to SpeakerVisits(LocalDate(2024, 3, 3), 1),
            "s3" to SpeakerVisits(LocalDate(2019, 5, 5), 2),
        )

        assertEquals(listOf(adam, dahl, cron, berg), state(visits).speakersByLastVisit)
    }

    @Test
    fun `planned visits come last`() {
        val visits = mapOf(
            "s1" to SpeakerVisits(LocalDate(2027, 1, 10), 1),
            "s2" to SpeakerVisits(LocalDate(2020, 1, 5), 1),
            "s3" to SpeakerVisits(LocalDate(2021, 1, 5), 1),
            "s4" to SpeakerVisits(LocalDate(2022, 1, 5), 1),
        )

        assertEquals(berg, state(visits).speakersByLastVisit.last())
    }

    @Test
    fun `no badge before visits are loaded`() {
        assertNull(state(null).speakerBadge("s1", today))
    }

    @Test
    fun `the badge counts the visits`() {
        val visits = mapOf("s1" to SpeakerVisits(LocalDate(2024, 3, 3), 3))

        assertEquals("vor 2 J. · 3×", state(visits).speakerBadge("s1", today)?.label)
        assertEquals("nie", state(visits).speakerBadge("s2", today)?.label)
    }

    @Test
    fun `the congregation of a speaker is named`() {
        assertEquals("Altdorf", state(null).congregationName(berg))
        assertEquals("", state(null).congregationName(berg.copy(congregationId = "x")))
    }
}
