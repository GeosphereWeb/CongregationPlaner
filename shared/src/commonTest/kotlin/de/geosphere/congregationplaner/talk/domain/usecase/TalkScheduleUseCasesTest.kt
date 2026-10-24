package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.testing.FakeTalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TalkScheduleUseCasesTest {
    private val repository = FakeTalkScheduleRepository()
    private val policy = TalkSchedulePermissionPolicy()
    private val save = SaveTalkSlotUseCase(repository, policy)
    private val delete = DeleteTalkSlotUseCase(repository, policy)

    private val admin = AppUser(
        uid = "1",
        approved = true,
        role = UserRole.CONGREGATION_ADMIN,
        congregation = testCongregation,
    )
    private val member = AppUser(uid = "2", approved = true, congregation = testCongregation)

    private val sunday = LocalDate(2026, 10, 25)
    private fun slot(date: LocalDate = sunday) = TalkSlot(date = date, time = LocalTime(17, 30))

    @Test
    fun `slot id is the iso date`() {
        assertEquals("2026-10-25", slot().id)
        assertEquals("2028-02-29", slot(LocalDate(2028, 2, 29)).id)
    }

    @Test
    fun `planner saves a slot in his own congregation`() = runBlocking {
        val result = save(admin, slot())

        assertTrue(result.isSuccess)
        assertEquals(listOf(slot()), repository.savedSlots)
        assertEquals(testCongregation, repository.requestedCongregation)
    }

    @Test
    fun `save is denied without permission or congregation`() = runBlocking {
        assertIs<TalkScheduleException.NotAllowed>(save(member, slot()).exceptionOrNull())
        assertIs<TalkScheduleException.NotAllowed>(save(admin.copy(congregation = null), slot()).exceptionOrNull())
        assertTrue(repository.savedSlots.isEmpty())
    }

    @Test
    fun `save accepts any weekday because the meeting day can move at short notice`() = runBlocking {
        assertTrue(save(admin, slot(LocalDate(2026, 10, 24))).isSuccess)
    }

    @Test
    fun `save checks the week event and trims its name`() = runBlocking {
        val invalid = listOf(
            slot().copy(weekEventId = "kongress", weekEventName = " "),
            slot().copy(weekEventId = "kongress", weekEventName = null),
            slot().copy(weekEventId = null, weekEventName = "Kongress"),
            slot().copy(weekEventId = "kongress", weekEventName = "x".repeat(TalkSlot.MAX_WEEK_EVENT_NAME_LENGTH + 1)),
        )
        invalid.forEach { assertIs<TalkScheduleException.InvalidWeekEvent>(save(admin, it).exceptionOrNull()) }

        save(admin, slot().copy(weekEventId = "kongress", weekEventName = " Kongress "))

        assertEquals("Kongress", repository.savedSlots.single().weekEventName)
    }

    @Test
    fun `repository failure is passed on`() = runBlocking {
        val failure = IllegalStateException("offline")
        repository.saveResult = Result.failure(failure)

        assertEquals(failure, save(admin, slot()).exceptionOrNull())
    }

    @Test
    fun `delete requires the permission`() = runBlocking {
        assertIs<TalkScheduleException.NotAllowed>(delete(member, sunday).exceptionOrNull())
        assertTrue(repository.deletedDates.isEmpty())

        assertTrue(delete(admin, sunday).isSuccess)
        assertEquals(listOf(sunday), repository.deletedDates)
    }

    @Test
    fun `move saves the new day first and then deletes the old one`() = runBlocking {
        val moved = slot(LocalDate(2026, 10, 24))

        val result = MoveTalkSlotUseCase(save, delete)(admin, from = sunday, slot = moved)

        assertEquals(moved, result.getOrThrow())
        assertEquals(listOf(moved), repository.savedSlots)
        assertEquals(listOf(sunday), repository.deletedDates)
    }

    @Test
    fun `move on the same day only saves and a failed save deletes nothing`() = runBlocking {
        val move = MoveTalkSlotUseCase(save, delete)

        assertTrue(move(admin, sunday, slot()).isSuccess)
        assertTrue(repository.deletedDates.isEmpty())

        repository.saveResult = Result.failure(IllegalStateException("offline"))
        assertTrue(move(admin, from = LocalDate(2026, 10, 18), slot = slot()).isFailure)
        assertTrue(repository.deletedDates.isEmpty())
    }

    @Test
    fun `move reports a failed delete of the old day`() = runBlocking {
        repository.deleteResult = Result.failure(IllegalStateException("offline"))

        val result = MoveTalkSlotUseCase(save, delete)(admin, from = sunday, slot = slot(LocalDate(2026, 10, 24)))

        assertEquals("offline", result.exceptionOrNull()?.message)
    }

    @Test
    fun `observe reads the year of the users congregation`() = runBlocking {
        repository.slots.value = listOf(slot())

        val slots = ObserveTalkYearUseCase(repository)(admin, 2026).first()

        assertEquals(listOf(slot()), slots)
        assertEquals(2026, repository.requestedYear)
        assertEquals(testCongregation, repository.requestedCongregation)
    }
}
