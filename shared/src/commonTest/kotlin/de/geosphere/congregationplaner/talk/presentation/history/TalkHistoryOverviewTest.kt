package de.geosphere.congregationplaner.talk.presentation.history

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TalkHistoryOverviewTest {
    private val d1 = LocalDate(2025, 1, 5)
    private val d2 = LocalDate(2025, 6, 1)
    private val d3 = LocalDate(2026, 2, 8)
    private val talks = listOf(Talk(1, "Eins"), Talk(2, "Zwei"), Talk(3, "Drei"), Talk(4, "Vier"), Talk(5, "Fünf"))
    private val history = listOf(
        TalkHistoryEntry(1, listOf(d1), speakers = mapOf(d1 to "Muster, Max")),
        TalkHistoryEntry(
            2,
            listOf(d1, d2, d3),
            speakers = mapOf(d1 to "Muster, Max", d2 to "Berg, Lea (Nord)", d3 to "Muster, Max"),
        ),
        TalkHistoryEntry(4, listOf(d2), speakers = mapOf(d2 to "Muster, Max")),
    )
    private val rows = talks.map { talk -> TalkHistoryRow(talk, history.firstOrNull { it.talkNumber == talk.number }) }

    @Test
    fun `talks are grouped by frequency with the most often first and never held last`() {
        val groups = rows.groupedByFrequency()

        assertEquals(listOf(3, 1, 0), groups.map { it.count })
        assertEquals(listOf(2), groups[0].rows.map { it.talk.number })
        assertEquals(listOf(1, 4), groups[1].rows.map { it.talk.number })
        assertEquals(listOf(3, 5), groups[2].rows.map { it.talk.number })
    }

    @Test
    fun `group titles name the count or that it was never held`() {
        assertEquals("3× gehalten", FrequencyGroup(3, emptyList()).title)
        assertEquals("Noch nie gehalten", FrequencyGroup(0, emptyList()).title)
    }

    @Test
    fun `no groups without talks`() {
        assertTrue(emptyList<TalkHistoryRow>().groupedByFrequency().isEmpty())
    }

    @Test
    fun `a speaker search lists his talks by number with the days newest first`() {
        val result = searchSpeakerHistory(talks, history, "muster")

        assertEquals(listOf("Muster, Max"), result.map { it.speaker })
        val max = result.single().talks
        assertEquals(listOf(1, 2, 4), max.map { it.talkNumber })
        assertEquals(listOf(d3, d1), max[1].days)
        assertEquals("Zwei", max[1].talk?.title)
    }

    @Test
    fun `the search finds guests by congregation too and sorts speakers by name`() {
        val result = searchSpeakerHistory(talks, history, "nord")
        assertEquals(listOf("Berg, Lea (Nord)"), result.map { it.speaker })

        assertEquals(
            listOf("Berg, Lea (Nord)", "Muster, Max"),
            searchSpeakerHistory(talks, history, "er").map {
                it.speaker
            },
        )
    }

    @Test
    fun `the search starts with the second letter`() {
        assertTrue(searchSpeakerHistory(talks, history, "m").isEmpty())
        assertTrue(searchSpeakerHistory(talks, history, " m ").isEmpty())
        assertEquals(1, searchSpeakerHistory(talks, history, "mu").size)
        assertTrue(searchSpeakerHistory(talks, history, "xyz").isEmpty())
    }

    @Test
    fun `a talk that left the catalog is still listed without a title`() {
        val result = searchSpeakerHistory(emptyList(), history, "muster")

        assertEquals(null, result.single().talks.first().talk)
        assertEquals(listOf(1, 2, 4), result.single().talks.map { it.talkNumber })
    }
}
