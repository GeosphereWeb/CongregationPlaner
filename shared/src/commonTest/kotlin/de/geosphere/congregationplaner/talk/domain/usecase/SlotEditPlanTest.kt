package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.SpeakerRequest
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.repository.ScheduleWrite
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SlotEditPlanTest {
    private val today = LocalDate(2026, 11, 24)
    private val day = LocalDate(2026, 12, 6)
    private val free = TalkSlot(day, LocalTime(10, 0))
    private val muster = free.copy(speakerMemberId = "m1", speakerLastName = "Muster", speakerFirstName = "Max")
    private val requested = muster.copy(requestId = "old")

    private fun plan(original: TalkSlot?, edited: TalkSlot, requested: Boolean, from: LocalDate? = original?.date) =
        planSlotEdit(SlotEdit(from, original, edited, requested), newId = "new", today = today)

    @Test
    fun `a new request creates the request and links it`() {
        val (write, saved) = plan(free, muster.copy(confirmed = true), requested = true).getOrThrow()

        assertEquals("new", saved.requestId)
        assertEquals(false, saved.confirmed)
        assertEquals(listOf(SpeakerRequest("new", today)), write.saveRequests)
        assertEquals(listOf(saved), write.saveSlots)
        assertEquals(emptyList(), write.deleteRequestIds)
    }

    @Test
    fun `the same speaker keeps the open request`() {
        val (write, saved) = plan(requested, requested.copy(note = "Beamer"), requested = true).getOrThrow()

        assertEquals("old", saved.requestId)
        assertEquals(ScheduleWrite(saveSlots = listOf(saved)), write)
    }

    @Test
    fun `another speaker replaces the open request`() {
        val other = requested.copy(speakerMemberId = "m2", speakerLastName = "Huber", speakerFirstName = "Jan")

        val (write, saved) = plan(requested, other, requested = true).getOrThrow()

        assertEquals("new", saved.requestId)
        assertEquals(listOf("old"), write.deleteRequestIds)
        assertEquals(listOf(SpeakerRequest("new", today)), write.saveRequests)
    }

    @Test
    fun `switching the request off accepts it and deletes the request`() {
        val (write, saved) = plan(requested, requested, requested = false).getOrThrow()

        assertNull(saved.requestId)
        assertEquals(listOf("old"), write.deleteRequestIds)
        assertEquals(emptyList(), write.saveRequests)
    }

    @Test
    fun `removing the speaker deletes the request`() {
        val (write, saved) = plan(requested, free.copy(requestId = "old"), requested = false).getOrThrow()

        assertNull(saved.requestId)
        assertEquals(listOf("old"), write.deleteRequestIds)
    }

    @Test
    fun `a request without speaker fails`() {
        assertIs<TalkScheduleException.InvalidRequest>(plan(free, free, requested = true).exceptionOrNull())
    }

    @Test
    fun `cancelling the talk of an open request fails`() {
        val result = plan(requested, requested.copy(talkCancelled = true), requested = true)

        assertIs<TalkScheduleException.InvalidRequest>(result.exceptionOrNull())
    }

    @Test
    fun `moving keeps the request and deletes the old day`() {
        val moved = requested.copy(date = LocalDate(2026, 12, 13))

        val (write, saved) = plan(requested, moved, requested = true).getOrThrow()

        assertEquals("old", saved.requestId)
        assertEquals(listOf(day), write.deleteSlotDates)
        assertEquals(emptyList(), write.deleteRequestIds)
    }

    @Test
    fun `a slot without request stays without request`() {
        val (write, saved) = plan(free, muster, requested = false).getOrThrow()

        assertNull(saved.requestId)
        assertEquals(ScheduleWrite(saveSlots = listOf(muster)), write)
    }
}
