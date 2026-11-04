package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.model.MeetingTimes
import de.geosphere.congregationplaner.congregation.testing.FakeCongregationMetadataRepository
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

class CreateYearSlotsUseCaseTest {
    private val planner = AppUser(
        uid = "1",
        approved = true,
        roles = setOf(CongregationRole.TALK_PLANNER),
        congregation = testCongregation,
    )
    private val member = AppUser(uid = "2", approved = true, congregation = testCongregation)

    // Standard: Sonntag 17:30; 2027 abweichend Samstag 09:30.
    private val congregation = CongregationMetadata(
        id = testCongregation.congregationId,
        weekendMeetingWeekday = 7,
        weekendMeetingTime = LocalTime(17, 30),
        yearlyMeetingTimes = mapOf(
            2027 to MeetingTimes(weekendWeekday = 6, weekendTime = LocalTime(9, 30)),
        ),
    )

    private fun useCase(
        repository: FakeTalkScheduleRepository,
        congregations: FakeCongregationMetadataRepository = FakeCongregationMetadataRepository(listOf(congregation)),
    ) = CreateYearSlotsUseCase(repository, congregations, TalkSchedulePermissionPolicy())

    @Test
    fun `creates a slot for every weekend day with the default time`() = runBlocking {
        val repository = FakeTalkScheduleRepository()
        val congregations = FakeCongregationMetadataRepository(listOf(congregation))

        val result = useCase(repository, congregations)(planner, 2026).getOrThrow()

        // The meeting times come from the circuit of the own congregation.
        assertEquals(listOf(testCongregation.circuitIds), congregations.requestedCircuits)

        assertEquals(YearSlotsResult(created = 52, alreadyExisting = 0), result)
        assertEquals(52, repository.createdSlots.size)
        assertTrue(repository.createdSlots.all { it.time == LocalTime(17, 30) && !it.talkCancelled && !it.confirmed })
        assertEquals(LocalDate(2026, 1, 4), repository.createdSlots.first().date)
        assertEquals(testCongregation, repository.requestedCongregation)
    }

    @Test
    fun `uses the weekday and time that are set for the year`() = runBlocking {
        val repository = FakeTalkScheduleRepository()

        useCase(repository)(planner, 2027).getOrThrow()

        assertEquals(LocalDate(2027, 1, 2), repository.createdSlots.first().date)
        assertTrue(repository.createdSlots.all { it.time == LocalTime(9, 30) })
    }

    @Test
    fun `an existing slot is never overwritten`() = runBlocking {
        val existing = TalkSlot(
            date = LocalDate(2026, 10, 25),
            time = LocalTime(10, 0),
            talkNumber = 12,
            talkTitle = "Der Weg zum Glück",
            note = "bleibt",
        )
        val repository = FakeTalkScheduleRepository(listOf(existing))

        val result = useCase(repository)(planner, 2026).getOrThrow()

        assertEquals(YearSlotsResult(created = 51, alreadyExisting = 1), result)
        assertTrue(repository.createdSlots.none { it.date == existing.date })
        assertEquals(existing, repository.slots.value.single { it.date == existing.date })
        assertTrue(repository.savedSlots.isEmpty())
    }

    @Test
    fun `a second run creates nothing`() = runBlocking {
        val repository = FakeTalkScheduleRepository()
        val useCase = useCase(repository)
        useCase(planner, 2026).getOrThrow()

        val second = useCase(planner, 2026).getOrThrow()

        assertEquals(YearSlotsResult(created = 0, alreadyExisting = 52), second)
        assertEquals(52, repository.createdSlots.size)
    }

    @Test
    fun `is denied without permission or congregation`() = runBlocking {
        val repository = FakeTalkScheduleRepository()

        assertIs<TalkScheduleException.NotAllowed>(useCase(repository)(member, 2026).exceptionOrNull())
        assertIs<TalkScheduleException.NotAllowed>(
            useCase(repository)(planner.copy(congregation = null), 2026).exceptionOrNull(),
        )
        assertTrue(repository.createdSlots.isEmpty())
    }

    @Test
    fun `fails when the congregation data is missing or cannot be read`() = runBlocking {
        val repository = FakeTalkScheduleRepository()
        val missing = FakeCongregationMetadataRepository(listOf(congregation.copy(id = "andere")))
        val broken = FakeCongregationMetadataRepository(error = IllegalStateException("offline"))

        val notFound = useCase(repository, missing)(planner, 2026).exceptionOrNull()
        assertIs<TalkScheduleException.CongregationNotFound>(notFound)
        assertEquals("offline", useCase(repository, broken)(planner, 2026).exceptionOrNull()?.message)
        assertTrue(repository.createdSlots.isEmpty())
    }

    @Test
    fun `a repository failure is passed on`() = runBlocking {
        val repository = FakeTalkScheduleRepository()
        repository.createMissingResult = Result.failure(IllegalStateException("denied"))

        assertEquals("denied", useCase(repository)(planner, 2026).exceptionOrNull()?.message)
    }
}
