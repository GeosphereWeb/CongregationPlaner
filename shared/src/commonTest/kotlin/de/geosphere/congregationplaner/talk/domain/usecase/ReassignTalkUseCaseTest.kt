package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.hasAssignment
import de.geosphere.congregationplaner.talk.domain.model.reassignTalk
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReassignTalkUseCaseTest {
    private val repository = FakeTalkScheduleRepository()
    private val reassign = ReassignTalkUseCase(repository, TalkSchedulePermissionPolicy())
    private val planner = AppUser(
        uid = "p",
        approved = true,
        roles = setOf(CongregationRole.TALK_PLANNER),
        congregation = testCongregation,
    )

    private val source = TalkSlot(
        date = LocalDate(2026, 11, 8),
        time = LocalTime(17, 30),
        weekEventId = "besuch",
        weekEventName = "Kreisaufseher",
        talkNumber = 12,
        talkTitle = "Der Weg zum Glück",
        speakerMemberId = "m1",
        speakerLastName = "Muster",
        speakerFirstName = "Max",
        chairmanMemberId = "c1",
        chairmanLastName = "Alt",
        chairmanFirstName = "Anton",
        confirmed = true,
        note = "Beamer",
    )
    private val freeDay = TalkSlot(LocalDate(2026, 11, 15), LocalTime(10, 0), note = "Saal 2")

    @Test
    fun `talk speaker and confirmation move while event time chairman and note stay with the day`() {
        val (newSource, newTarget) = reassignTalk(source, freeDay)

        val emptiedSource = source.copy(
            talkNumber = null,
            talkTitle = null,
            speakerMemberId = null,
            speakerLastName = null,
            speakerFirstName = null,
            confirmed = false,
        )
        assertEquals(emptiedSource, newSource)
        assertFalse(newSource.hasAssignment)
        assertEquals(
            freeDay.copy(
                talkNumber = 12,
                talkTitle = "Der Weg zum Glück",
                speakerMemberId = "m1",
                speakerLastName = "Muster",
                speakerFirstName = "Max",
                confirmed = true,
            ),
            newTarget,
        )
    }

    @Test
    fun `a talk moves to a free day and both slots are saved together`() = runBlocking {
        val (newSource, newTarget) = reassign(planner, source, freeDay).getOrThrow()

        assertFalse(newSource.hasAssignment)
        assertEquals(12, newTarget.talkNumber)
        assertEquals(listOf(newSource, newTarget), repository.savedSlots)
        assertEquals(testCongregation, repository.requestedCongregation)
    }

    @Test
    fun `two planned days swap their talks and speakers`() = runBlocking {
        val other = freeDay.copy(
            talkNumber = 31,
            talkTitle = "Glück im Alltag",
            speakerLastName = "Gast",
            speakerFirstName = "",
        )

        val (newSource, newTarget) = reassign(planner, source, other).getOrThrow()

        assertEquals(31, newSource.talkNumber)
        assertEquals("Gast", newSource.freeSpeakerName)
        assertEquals(12, newTarget.talkNumber)
        assertEquals("m1", newTarget.speakerMemberId)
    }

    @Test
    fun `reassigning is refused for invalid requests and nothing is saved`() = runBlocking {
        val member = planner.copy(roles = emptySet())
        assertIs<TalkScheduleException.NotAllowed>(reassign(member, source, freeDay).exceptionOrNull())
        assertIs<TalkScheduleException.NotAllowed>(
            reassign(planner.copy(congregation = null), source, freeDay).exceptionOrNull(),
        )
        assertIs<TalkScheduleException.SameDay>(reassign(planner, source, source).exceptionOrNull())
        assertIs<TalkScheduleException.NothingToReassign>(reassign(planner, freeDay, source).exceptionOrNull())
        assertIs<TalkScheduleException.TargetCancelled>(
            reassign(planner, source, freeDay.copy(talkCancelled = true)).exceptionOrNull(),
        )
        // The speaker would be chairman on the target day.
        assertIs<TalkScheduleException.SpeakerIsChairman>(
            reassign(
                planner,
                source,
                freeDay.copy(chairmanMemberId = "m1", chairmanLastName = "Muster", chairmanFirstName = "Max"),
            )
                .exceptionOrNull(),
        )
        assertTrue(repository.savedSlots.isEmpty())
    }

    @Test
    fun `a failed write is reported`() = runBlocking {
        repository.saveResult = Result.failure(IllegalStateException("denied"))

        assertEquals("denied", reassign(planner, source, freeDay).exceptionOrNull()?.message)
    }
}
