package de.geosphere.congregationplaner.talk.presentation.history

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TalkHistoryTextsTest {
    @Test
    fun `the days of a speaker are listed with weekday`() {
        val days = SpeakerTalkDays(1, Talk(1, "Eins"), listOf(LocalDate(2026, 10, 25), LocalDate(2026, 11, 8)))

        assertEquals("So, 25.10.2026, So, 08.11.2026", days.daysText())
    }

    @Test
    fun `talks without history count as never held`() {
        val state = TalkHistoryUiState(
            talks = listOf(Talk(1, "Eins"), Talk(2, "Zwei"), Talk(3, "Drei")),
            history = listOf(TalkHistoryEntry(2, listOf(LocalDate(2026, 1, 4)))),
        )

        assertEquals(2, state.neverHeldCount)
    }
}
