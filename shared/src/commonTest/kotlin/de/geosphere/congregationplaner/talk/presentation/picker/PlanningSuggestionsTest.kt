package de.geosphere.congregationplaner.talk.presentation.picker

import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.MemberRef
import de.geosphere.congregationplaner.talk.domain.model.PlanningUsage
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkPlanningSettings
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlanningSuggestionsTest {
    private val cutoff = LocalDate(2025, 6, 1)
    private val talks = listOf(Talk(1, "Eins"), Talk(2, "Zwei"), Talk(3, "Drei"), Talk(4, "Vier"))
    private val usage = PlanningUsage(
        settings = TalkPlanningSettings(speakerFreeSince = cutoff, talkFreeSince = cutoff),
        talkLastPlanned = mapOf(1 to LocalDate(2025, 1, 5), 2 to LocalDate(2026, 3, 1), 4 to LocalDate(2023, 2, 5)),
        memberLastPlanned = mapOf("m1" to LocalDate(2025, 7, 6)),
        externalLastPlanned = mapOf("e2" to LocalDate(2024, 4, 7)),
    )

    @Test
    fun `free talks come first and the longest unplanned ones lead`() {
        val suggestions = talks.suggested(usage, showBlocked = false)

        assertEquals(listOf(3, 4, 1), suggestions.map { it.item.number })
        assertEquals("noch nie eingeplant", suggestions[0].note)
        assertEquals("zuletzt eingeplant So, 05.02.2023", suggestions[1].note)
    }

    @Test
    fun `blocked talks are shown last with the reason when asked for`() {
        val suggestions = talks.suggested(usage, showBlocked = true)

        assertEquals(listOf(3, 4, 1, 2), suggestions.map { it.item.number })
        assertEquals(true, suggestions.last().blocked)
        assertEquals("gesperrt – eingeplant So, 01.03.2026", suggestions.last().note)
    }

    @Test
    fun `members and external speakers follow the same order`() {
        val members = listOf(MemberRef("m1", "Muster", "Max"), MemberRef("m2", "Beispiel", "Ben"))
        val externals =
            listOf(ExternalSpeaker("e1", "Extern", "Eva", "Nord"), ExternalSpeaker("e2", "Alt", "Al", "Süd"))

        assertEquals(listOf("m2"), members.suggestedMembers(usage, showBlocked = false).map { it.item.memberId })
        assertEquals(listOf("m2", "m1"), members.suggestedMembers(usage, showBlocked = true).map { it.item.memberId })
        assertEquals(listOf("e1", "e2"), externals.suggestedExternal(usage, showBlocked = false).map { it.item.id })
    }

    @Test
    fun `external speakers are limited to the chosen talk and talks to the chosen speaker`() {
        val known = ExternalSpeaker("e1", "Extern", "Eva", "Nord", talkNumbers = listOf(2, 3))
        val unknown = ExternalSpeaker("e2", "Alt", "Al", "Süd")

        assertEquals(listOf(known, unknown), listOf(known, unknown).offering(null))
        assertEquals(listOf(known, unknown), listOf(known, unknown).offering(3))
        assertEquals(listOf(unknown), listOf(known, unknown).offering(4))
        assertEquals(listOf(2, 3), talks.offeredBy(known).map { it.number })
        assertEquals(talks, talks.offeredBy(unknown))
        assertEquals(talks, talks.offeredBy(null))
    }

    @Test
    fun `the selected external speaker is found by the id of the slot`() {
        val known = ExternalSpeaker("e1", "Extern", "Eva", "Nord")
        val slot = TalkSlot(LocalDate(2026, 1, 4), LocalTime(10, 0))

        assertEquals(known, slot.copy(externalSpeakerId = "e1").selectedExternal(listOf(known)))
        assertNull(slot.copy(externalSpeakerId = "gone").selectedExternal(listOf(known)))
        assertNull(slot.selectedExternal(listOf(known)))
    }
}
