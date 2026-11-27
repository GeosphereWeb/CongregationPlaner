package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.SpeakerVisits
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.testing.FakeTalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ObserveExternalSpeakerVisitsUseCaseTest {
    private val day = LocalDate(2025, 3, 2)
    private val repository = FakeTalkScheduleRepository(
        listOf(TalkSlot(date = day, time = LocalTime(10, 0), speakerLastName = "Berg", externalSpeakerId = "e1")),
    )
    private val useCase = ObserveExternalSpeakerVisitsUseCase(repository)

    @Test
    fun `visits come from the schedule of the congregation`() = runBlocking {
        val user = AppUser(uid = "u", approved = true, congregation = testCongregation)

        assertEquals(mapOf("e1" to SpeakerVisits(day, 1)), useCase(user).first())
        assertEquals(testCongregation, repository.requestedCongregation)
    }

    @Test
    fun `a user without congregation gets no visits`() = runBlocking {
        assertTrue(useCase(AppUser(uid = "u", approved = true)).toList().isEmpty())
    }

    @Test
    fun `visits are read once and not kept polling`() = runTest {
        val user = AppUser(uid = "u", approved = true, congregation = testCongregation)

        val all = withTimeout(1.seconds) { useCase(user).toList() }

        assertEquals(1, all.size)
    }
}
