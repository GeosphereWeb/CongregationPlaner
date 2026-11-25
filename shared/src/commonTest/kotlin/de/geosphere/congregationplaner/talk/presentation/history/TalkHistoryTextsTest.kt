package de.geosphere.congregationplaner.talk.presentation.history

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.usecase.BuildTalkHistoryOverviewUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.FrequencyGroup
import de.geosphere.congregationplaner.talk.domain.usecase.SpeakerTalkDays
import de.geosphere.congregationplaner.talk.domain.usecase.TalkHistoryRow
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TalkHistoryTextsTest {
    @Test
    fun `the days of a speaker are listed with weekday`() {
        val days = SpeakerTalkDays(1, Talk(1, "Eins"), listOf(LocalDate(2026, 10, 25), LocalDate(2026, 11, 8)))

        assertEquals("So, 25.10.2026, So, 08.11.2026", days.daysText())
    }

    @Test
    fun `group titles name the count or that it was never held`() {
        assertEquals("3× gehalten", FrequencyGroup(3, emptyList()).title)
        assertEquals("Noch nie gehalten", FrequencyGroup(0, emptyList()).title)
    }

    @Test
    fun `talks without history count as never held`() {
        val overview = BuildTalkHistoryOverviewUseCase()(
            listOf(Talk(1, "Eins"), Talk(2, "Zwei"), Talk(3, "Drei")),
            listOf(TalkHistoryEntry(2, listOf(LocalDate(2026, 1, 4)))),
        )

        assertEquals(2, TalkHistoryUiState(overview = overview).neverHeldCount)
    }

    @Test
    fun `an inactive talk is dimmed in the history`() {
        assertEquals(1f, historyRowAlpha(Talk(1, "Eins")))
        assertEquals(1f, historyRowAlpha(null))
        assertEquals(0.5f, historyRowAlpha(Talk(2, "Zwei", active = false)))
    }

    @Test
    fun `a history row badge counts all held days`() {
        val today = LocalDate(2026, 11, 25)
        val days = listOf(LocalDate(2019, 3, 10), LocalDate(2021, 11, 2))
        val held = TalkHistoryRow(Talk(1, "Eins"), TalkHistoryEntry(1, days))

        assertEquals("vor 5 J. · 2×", held.badge(today).label)
        assertEquals("nie", TalkHistoryRow(Talk(2, "Zwei"), null).badge(today).label)
    }

    @Test
    fun `only an inactive talk counts as inactive`() {
        assertTrue(isInactive(Talk(2, "Zwei", active = false)))
        assertFalse(isInactive(Talk(1, "Eins")))
        assertFalse(isInactive(null))
    }
}
