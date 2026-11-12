package de.geosphere.congregationplaner.talk.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlanningUsageTest {
    private val cutoff = LocalDate(2025, 6, 1)
    private val settings = TalkPlanningSettings(speakerFreeSince = cutoff, talkFreeSince = cutoff)

    private fun slot(day: LocalDate, number: Int? = null, member: String? = null, external: String? = null) = TalkSlot(
        date = day,
        time = LocalTime(10, 0),
        talkNumber = number,
        talkTitle = number?.let { "Vortrag $it" },
        speakerMemberId = member,
        speakerLastName = (member ?: external)?.let { "Name $it" },
        speakerFirstName = (member ?: external)?.let { "" },
        externalSpeakerId = external,
        speakerCongregation = external?.let { "Nord" },
    )

    @Test
    fun `the latest planned day counts for members external speakers and talks`() {
        val usage = planningUsageOf(
            listOf(
                slot(LocalDate(2024, 3, 3), number = 12, member = "m1"),
                slot(LocalDate(2025, 1, 5), number = 12, member = "m1"),
                slot(LocalDate(2024, 9, 8), number = 31, external = "e1"),
            ),
            settings,
            excludedDate = null,
        )

        assertEquals(mapOf("m1" to LocalDate(2025, 1, 5)), usage.memberLastPlanned)
        assertEquals(mapOf("e1" to LocalDate(2024, 9, 8)), usage.externalLastPlanned)
        assertEquals(mapOf(12 to LocalDate(2025, 1, 5), 31 to LocalDate(2024, 9, 8)), usage.talkLastPlanned)
    }

    @Test
    fun `a day on the cut-off date or later blocks and the day before does not`() {
        val usage = planningUsageOf(
            listOf(
                slot(cutoff, number = 1, member = "on"),
                slot(LocalDate(2025, 5, 31), number = 2, member = "before"),
            ),
            settings,
            excludedDate = null,
        )

        assertTrue(usage.isMemberBlocked("on"))
        assertTrue(usage.isTalkBlocked(1))
        assertFalse(usage.isMemberBlocked("before"))
        assertFalse(usage.isTalkBlocked(2))
        assertFalse(usage.isMemberBlocked("never"))
        assertFalse(usage.isTalkBlocked(99))
    }

    @Test
    fun `a planned day in the future blocks as well`() {
        val usage = planningUsageOf(listOf(slot(LocalDate(2027, 2, 7), number = 5, external = "e1")), settings, null)

        assertTrue(usage.isExternalBlocked("e1"))
        assertTrue(usage.isTalkBlocked(5))
    }

    @Test
    fun `the edited slot and cancelled talks do not count`() {
        val edited = slot(LocalDate(2025, 8, 3), number = 7, member = "m1")
        val cancelled = slot(LocalDate(2025, 9, 7), number = 8, member = "m2").copy(talkCancelled = true)

        val usage = planningUsageOf(listOf(edited, cancelled), settings, excludedDate = edited.date)

        assertTrue(usage.memberLastPlanned.isEmpty())
        assertTrue(usage.talkLastPlanned.isEmpty())
        assertFalse(usage.isMemberBlocked("m1"))
        assertFalse(usage.isTalkBlocked(8))
    }

    @Test
    fun `without cut-off dates nothing is blocked`() {
        val usage =
            planningUsageOf(
                listOf(slot(LocalDate(2026, 1, 4), number = 3, member = "m1")),
                TalkPlanningSettings(),
                null,
            )

        assertFalse(usage.isMemberBlocked("m1"))
        assertFalse(usage.isTalkBlocked(3))
        assertEquals(LocalDate(2026, 1, 4), usage.memberLastPlanned["m1"])
    }

    @Test
    fun `free entries are neither counted nor blocked`() {
        val free = slot(
            LocalDate(2025, 7, 6),
        ).copy(talkTitle = "Sondervortrag", speakerLastName = "Gast", speakerFirstName = "")

        val usage = planningUsageOf(listOf(free), settings, null)

        assertEquals(PlanningUsage(settings), usage)
    }
}
