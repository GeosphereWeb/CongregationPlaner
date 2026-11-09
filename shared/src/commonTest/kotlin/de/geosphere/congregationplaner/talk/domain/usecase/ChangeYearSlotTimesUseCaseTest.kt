package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.testing.FakeTalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChangeYearSlotTimesUseCaseTest {
    private val planner = AppUser(
        uid = "1",
        approved = true,
        roles = setOf(CongregationRole.TALK_PLANNER),
        congregation = testCongregation,
    )
    private val member = AppUser(uid = "2", approved = true, congregation = testCongregation)
    private val newTime = LocalTime(9, 30)

    private val planned = TalkSlot(
        date = LocalDate(2027, 1, 3),
        time = LocalTime(17, 30),
        talkNumber = 12,
        talkTitle = "Der Weg zum Glück",
        note = "bleibt",
        confirmed = true,
    )
    private val empty = TalkSlot(LocalDate(2027, 1, 10), LocalTime(17, 30))
    private val alreadyRight = TalkSlot(LocalDate(2027, 1, 17), newTime)

    private fun useCase(repository: FakeTalkScheduleRepository) =
        ChangeYearSlotTimesUseCase(repository, TalkSchedulePermissionPolicy())

    @Test
    fun `sets the time on every slot of the year and keeps all other fields`() = runBlocking {
        val repository = FakeTalkScheduleRepository(listOf(planned, empty, alreadyRight))

        val changed = useCase(repository)(planner, 2027, newTime).getOrThrow()

        assertEquals(2, changed)
        assertEquals(listOf(planned.copy(time = newTime), empty.copy(time = newTime)), repository.savedSlots)
        assertEquals(2027, repository.requestedYear)
        assertEquals(testCongregation, repository.requestedCongregation)
    }

    @Test
    fun `slots that already have the time are not written`() = runBlocking {
        val repository = FakeTalkScheduleRepository(listOf(alreadyRight))

        assertEquals(0, useCase(repository)(planner, 2027, newTime).getOrThrow())
        assertTrue(repository.savedSlots.isEmpty())
    }

    @Test
    fun `a failing write stops the change and reports the error`() = runBlocking {
        val repository = FakeTalkScheduleRepository(listOf(planned, empty))
        repository.saveResult = Result.failure(IllegalStateException("denied"))

        val result = useCase(repository)(planner, 2027, newTime)

        assertEquals("denied", result.exceptionOrNull()?.message)
        assertTrue(repository.savedSlots.isEmpty())
    }

    @Test
    fun `a failing read reports the error`() = runBlocking {
        val repository = FakeTalkScheduleRepository(listOf(planned))
        repository.observeError = IllegalStateException("offline")

        val result = useCase(repository)(planner, 2027, newTime)

        assertEquals("offline", result.exceptionOrNull()?.message)
    }

    @Test
    fun `needs the permission to plan talks`() = runBlocking {
        val repository = FakeTalkScheduleRepository(listOf(planned))

        val result = useCase(repository)(member, 2027, newTime)

        assertIs<TalkScheduleException.NotAllowed>(result.exceptionOrNull())
        assertTrue(repository.savedSlots.isEmpty())
    }

    @Test
    fun `needs a congregation`() {
        val result = runBlocking {
            useCase(FakeTalkScheduleRepository())(planner.copy(congregation = null), 2027, newTime)
        }

        assertIs<TalkScheduleException.NotAllowed>(result.exceptionOrNull())
    }
}
