package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.SpeakerRequest
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.testing.FakeSpeakerRequestRepository
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

class SpeakerRequestUseCasesTest {
    private val policy = TalkSchedulePermissionPolicy()
    private val today = LocalDate(2026, 11, 24)
    private val day = LocalDate(2026, 12, 6)
    private val free = TalkSlot(day, LocalTime(10, 0))
    private val muster = free.copy(speakerMemberId = "m1", speakerLastName = "Muster", speakerFirstName = "Max")
    private val planner = AppUser(
        uid = "p",
        approved = true,
        roles = setOf(CongregationRole.TALK_PLANNER),
        congregation = testCongregation,
    )
    private val helper = planner.copy(uid = "h", roles = setOf(CongregationRole.TALK_HELPER))

    private val schedule = FakeTalkScheduleRepository(listOf(free))
    private val requests = FakeSpeakerRequestRepository(schedule = schedule)

    @Test
    fun `saving an edited slot with request needs a planner`() = runBlocking {
        val result = SaveEditedSlotUseCase(requests, policy)(helper, day, free, muster, requested = true, today)

        assertIs<TalkScheduleException.NotAllowed>(result.exceptionOrNull())
        assertTrue(requests.writes.isEmpty())
    }

    @Test
    fun `saving an edited slot as request writes slot and request in one batch`() = runBlocking {
        val saved = SaveEditedSlotUseCase(requests, policy)(planner, day, free, muster, requested = true, today)
            .getOrThrow()

        assertEquals(1, requests.writes.size)
        assertEquals(saved, schedule.slots.value.single())
        assertEquals(saved.requestId, requests.requests.value.single().id)
        assertEquals(20, saved.requestId?.length)
    }

    @Test
    fun `saving checks the slot like before`() = runBlocking {
        val invalid = muster.copy(note = "x".repeat(TalkSlot.MAX_NOTE_LENGTH + 1))

        val result = SaveEditedSlotUseCase(requests, policy)(planner, day, free, invalid, requested = false, today)

        assertIs<TalkScheduleException.InvalidNote>(result.exceptionOrNull())
        assertTrue(requests.writes.isEmpty())
    }

    @Test
    fun `deleting a requested slot deletes its request too`() = runBlocking {
        val requested = muster.copy(requestId = "r1")
        schedule.slots.value = listOf(requested)
        requests.requests.value = listOf(SpeakerRequest("r1", today))

        DeleteTalkSlotUseCase(schedule, requests, policy)(planner, day, requestId = "r1").getOrThrow()

        assertTrue(schedule.slots.value.isEmpty())
        assertTrue(requests.requests.value.isEmpty())
    }

    @Test
    fun `deleting a slot without request deletes only the slot`() = runBlocking {
        DeleteTalkSlotUseCase(schedule, requests, policy)(planner, day).getOrThrow()

        assertEquals(listOf(day), schedule.deletedDates)
        assertTrue(requests.writes.isEmpty())
    }
}
