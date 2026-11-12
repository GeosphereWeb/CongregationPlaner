package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.talkHistoryOf
import de.geosphere.congregationplaner.talk.testing.FakeTalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TalkHistoryUseCasesTest {
    private val spring = LocalDate(2025, 3, 1)
    private val autumn = LocalDate(2026, 10, 25)
    private val future = LocalDate(2027, 2, 7)

    private fun slot(day: LocalDate, number: Int? = 7, cancelled: Boolean = false) = TalkSlot(
        date = day,
        time = LocalTime(17, 30),
        talkCancelled = cancelled,
        talkNumber = number,
        talkTitle = number?.let { "Vortrag $it" },
    )

    @Test
    fun `last held day is the latest date and missing for no dates`() {
        assertEquals(autumn, TalkHistoryEntry(7, listOf(spring, autumn)).lastHeldOn)
        assertNull(TalkHistoryEntry(7, emptyList()).lastHeldOn)
    }

    @Test
    fun `history groups the days of each talk over all years including planned future days`() {
        val history = talkHistoryOf(listOf(slot(autumn), slot(spring, number = 3), slot(future), slot(spring)))

        assertEquals(
            listOf(TalkHistoryEntry(3, listOf(spring)), TalkHistoryEntry(7, listOf(spring, autumn, future))),
            history,
        )
    }

    @Test
    fun `slots without talk and cancelled talks do not count`() {
        val history = talkHistoryOf(listOf(slot(spring, number = null), slot(autumn, cancelled = true)))

        assertTrue(history.isEmpty())
    }

    @Test
    fun `the speaker of each day is a member or an external speaker with congregation`() {
        val member = slot(spring).copy(speakerMemberId = "m1", speakerLastName = "Muster", speakerFirstName = "Max")
        val guest = slot(autumn).copy(
            externalSpeakerId = "x1",
            speakerLastName = "Berg",
            speakerFirstName = "Lea",
            speakerCongregation = "Musterstadt",
        )

        val entry = talkHistoryOf(listOf(member, guest, slot(future))).single()

        assertEquals(mapOf(spring to "Muster, Max", autumn to "Berg, Lea (Musterstadt)"), entry.speakers)
    }

    @Test
    fun `observe derives the history from all slots of the congregation and is empty without one`() = runBlocking {
        val schedule = FakeTalkScheduleRepository(listOf(slot(autumn, number = 9), slot(spring, number = 2)))
        val observe = ObserveTalkHistoryUseCase(schedule)
        val user = AppUser(uid = "1", approved = true, congregation = testCongregation)

        assertEquals(listOf(2, 9), observe(user).first().map { it.talkNumber })
        assertEquals(testCongregation, schedule.requestedCongregation)
        assertTrue(observe(user.copy(congregation = null)).toList().isEmpty())
    }

    @Test
    fun `a free speaker is named in the history and a free talk without number is left out`() {
        val freeSpeaker = slot(spring).copy(speakerLastName = "Gast (Hamburg)", speakerFirstName = "")
        val freeTalk = slot(autumn, number = null).copy(talkTitle = "Sondervortrag")

        val history = talkHistoryOf(listOf(freeSpeaker, freeTalk))

        assertEquals(listOf(TalkHistoryEntry(7, listOf(spring), mapOf(spring to "Gast (Hamburg)"))), history)
    }
}
