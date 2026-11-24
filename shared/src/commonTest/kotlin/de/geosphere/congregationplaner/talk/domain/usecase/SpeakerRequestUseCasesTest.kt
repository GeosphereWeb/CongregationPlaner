package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.ContactAttempt
import de.geosphere.congregationplaner.talk.domain.model.ContactKind
import de.geosphere.congregationplaner.talk.domain.model.OpenRequest
import de.geosphere.congregationplaner.talk.domain.model.SpeakerRequest
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.testing.FakeSpeakerRequestRepository
import de.geosphere.congregationplaner.talk.testing.FakeTalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
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
    private val saveEdited = SaveEditedSlotUseCase(requests, policy)

    @Test
    fun `saving an edited slot with request needs a planner`() = runBlocking {
        val result = saveEdited(helper, SlotEdit(day, free, muster, requested = true), today)

        assertIs<TalkScheduleException.NotAllowed>(result.exceptionOrNull())
        assertTrue(requests.writes.isEmpty())
    }

    @Test
    fun `saving an edited slot as request writes slot and request in one batch`() = runBlocking {
        val saved = saveEdited(planner, SlotEdit(day, free, muster, requested = true), today)
            .getOrThrow()

        assertEquals(1, requests.writes.size)
        assertEquals(saved, schedule.slots.value.single())
        assertEquals(saved.requestId, requests.requests.value.single().id)
        assertEquals(20, saved.requestId?.length)
    }

    @Test
    fun `saving checks the slot like before`() = runBlocking {
        val invalid = muster.copy(note = "x".repeat(TalkSlot.MAX_NOTE_LENGTH + 1))

        val result = saveEdited(planner, SlotEdit(day, free, invalid, requested = false), today)

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

    private val undated =
        SpeakerRequest("u1", LocalDate(2026, 11, 20), speakerLastName = "Huber", speakerFirstName = "")
    private val requestedSlot = muster.copy(requestId = "r1", talkNumber = 12, talkTitle = "Zwölf", note = "Saal")
    private val datedRequest = SpeakerRequest("r1", LocalDate(2026, 11, 10))

    private fun openOf(request: SpeakerRequest, slot: TalkSlot?) = OpenRequest(request, slot, false, false)

    @Test
    fun `helpers observe open requests but only planners may change them`(): Unit = runBlocking {
        schedule.slots.value = listOf(requestedSlot)
        requests.requests.value = listOf(datedRequest, undated)

        val open = ObserveOpenRequestsUseCase(requests, schedule, policy)(helper, today).first().open
        val write = SaveSpeakerRequestUseCase(requests, policy)(helper, undated, today)

        assertEquals(listOf("r1", "u1"), open.map { it.request.id })
        assertIs<TalkScheduleException.NotAllowed>(write.exceptionOrNull())
    }

    @Test
    fun `members without planning role see no requests`() = runBlocking {
        requests.requests.value = listOf(undated)
        val member = planner.copy(uid = "m", roles = emptySet())

        assertTrue(ObserveOpenRequestsUseCase(requests, schedule, policy)(member, today).first().open.isEmpty())
    }

    @Test
    fun `observe marks a request on a past day as passed`() = runBlocking {
        schedule.slots.value = listOf(requestedSlot.copy(date = LocalDate(2026, 11, 1)))
        requests.requests.value = listOf(datedRequest)

        val open = ObserveOpenRequestsUseCase(requests, schedule, policy)(planner, today).first().open
        assertTrue(open.single().datePassed)
    }

    @Test
    fun `a new undated request gets an id and today as creation day`() = runBlocking {
        val saved = SaveSpeakerRequestUseCase(requests, policy)(planner, undated.copy(id = ""), today).getOrThrow()

        assertEquals(20, saved.id.length)
        assertEquals(today, saved.createdOn)
        assertEquals(listOf(saved), requests.requests.value)
    }

    @Test
    fun `an undated request needs a speaker and a short note`(): Unit = runBlocking {
        val save = SaveSpeakerRequestUseCase(requests, policy)
        val longNote = undated.copy(note = "x".repeat(SpeakerRequest.MAX_NOTE_LENGTH + 1))

        val noSpeaker = save(planner, SpeakerRequest("", today), today)
        assertIs<TalkScheduleException.InvalidSpeaker>(noSpeaker.exceptionOrNull())
        assertIs<TalkScheduleException.InvalidNote>(save(planner, longNote, today).exceptionOrNull())
    }

    @Test
    fun `a contact attempt is appended with a trimmed note`() = runBlocking {
        requests.requests.value = listOf(undated)

        AddContactAttemptUseCase(requests, policy)(planner, undated, ContactAttempt(today, ContactKind.VOICEMAIL, "  "))
            .getOrThrow()

        assertEquals(
            listOf(ContactAttempt(today, ContactKind.VOICEMAIL)),
            requests.requests.value.single().contactAttempts,
        )
    }

    @Test
    fun `a contact attempt with a long note or beyond the limit fails`() = runBlocking {
        val add = AddContactAttemptUseCase(requests, policy)
        val longNote = ContactAttempt(today, ContactKind.CALL, "x".repeat(SpeakerRequest.MAX_ATTEMPT_NOTE_LENGTH + 1))
        val full = undated.copy(
            contactAttempts = List(SpeakerRequest.MAX_ATTEMPTS) { ContactAttempt(today, ContactKind.CALL) },
        )

        assertIs<TalkScheduleException.InvalidContactAttempt>(add(planner, undated, longNote).exceptionOrNull())
        assertIs<TalkScheduleException.InvalidContactAttempt>(
            add(planner, full, ContactAttempt(today, ContactKind.CALL)).exceptionOrNull(),
        )
        assertTrue(requests.writes.isEmpty())
    }

    @Test
    fun `accepting clears the request id and deletes the request`() = runBlocking {
        schedule.slots.value = listOf(requestedSlot)
        requests.requests.value = listOf(datedRequest)

        AcceptSpeakerRequestUseCase(requests, policy)(planner, openOf(datedRequest, requestedSlot)).getOrThrow()

        assertNull(schedule.slots.value.single().requestId)
        assertEquals("Muster", schedule.slots.value.single().speakerLastName)
        assertTrue(requests.requests.value.isEmpty())
    }

    @Test
    fun `accepting a request on a past day works`() = runBlocking {
        val past = requestedSlot.copy(date = LocalDate(2026, 11, 1))
        schedule.slots.value = listOf(past)
        requests.requests.value = listOf(datedRequest)

        AcceptSpeakerRequestUseCase(requests, policy)(planner, openOf(datedRequest, past)).getOrThrow()

        assertNull(schedule.slots.value.single().requestId)
    }

    @Test
    fun `an undated request cannot be accepted`(): Unit = runBlocking {
        val result = AcceptSpeakerRequestUseCase(requests, policy)(planner, openOf(undated, null))

        assertIs<TalkScheduleException.InvalidRequest>(result.exceptionOrNull())
    }

    @Test
    fun `declining frees the day but keeps time and note`() = runBlocking {
        schedule.slots.value = listOf(requestedSlot)
        requests.requests.value = listOf(datedRequest)

        DeclineSpeakerRequestUseCase(requests, policy)(planner, openOf(datedRequest, requestedSlot)).getOrThrow()

        assertEquals(free.copy(note = "Saal"), schedule.slots.value.single())
        assertTrue(requests.requests.value.isEmpty())
    }

    @Test
    fun `declining an undated request deletes it`() = runBlocking {
        requests.requests.value = listOf(undated)

        DeclineSpeakerRequestUseCase(requests, policy)(planner, openOf(undated, null)).getOrThrow()

        assertTrue(requests.requests.value.isEmpty())
        assertEquals(listOf(free), schedule.slots.value)
    }

    @Test
    fun `assigning a day moves speaker and talk from the request to the slot`() = runBlocking {
        val withTalk = undated.copy(talkNumber = 12, talkTitle = "Zwölf", note = "Frühjahr")
        requests.requests.value = listOf(withTalk)

        AssignRequestToDateUseCase(requests, policy)(planner, withTalk, free).getOrThrow()

        val slot = schedule.slots.value.single()
        assertEquals("u1", slot.requestId)
        assertEquals("Huber", slot.speakerLastName)
        assertEquals(12, slot.talkNumber)
        assertEquals(SpeakerRequest("u1", withTalk.createdOn, note = "Frühjahr"), requests.requests.value.single())
    }

    @Test
    fun `assigning a taken or cancelled day fails`() = runBlocking {
        val assign = AssignRequestToDateUseCase(requests, policy)

        assertIs<TalkScheduleException.TargetTaken>(assign(planner, undated, muster).exceptionOrNull())
        assertIs<TalkScheduleException.TargetCancelled>(
            assign(planner, undated, free.copy(talkCancelled = true)).exceptionOrNull(),
        )
        assertTrue(requests.writes.isEmpty())
    }
}
