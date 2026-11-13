package de.geosphere.congregationplaner.talk.presentation.history

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.usecase.BuildTalkHistoryOverviewUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.FrequencyGroup
import de.geosphere.congregationplaner.talk.domain.usecase.SpeakerTalkDays
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
}
