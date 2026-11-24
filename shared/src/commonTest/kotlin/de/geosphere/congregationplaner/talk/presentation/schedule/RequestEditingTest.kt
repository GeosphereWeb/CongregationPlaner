package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RequestEditingTest {
    private val free = TalkSlot(LocalDate(2026, 12, 6), LocalTime(10, 0))
    private val muster = free.copy(speakerMemberId = "m1", speakerLastName = "Muster", speakerFirstName = "Max")
    private val requested = muster.copy(requestId = "r1")

    @Test
    fun `switching the request on keeps an open request and clears informed`() {
        assertEquals("r1", requested.withRequested(true, requested).requestId)
        val marked = muster.copy(confirmed = true).withRequested(true, muster)
        assertTrue(marked.isRequested)
        assertFalse(marked.confirmed)
        assertNull(requested.withRequested(false, requested).requestId)
    }

    @Test
    fun `only a slot with speaker and a held talk can be requested`() {
        assertTrue(muster.canBeRequested)
        assertFalse(free.canBeRequested)
        assertFalse(muster.copy(talkCancelled = true).canBeRequested)
    }

    @Test
    fun `replacing the speaker of an open request warns`() {
        val other = requested.copy(speakerMemberId = "m2", speakerLastName = "Huber", speakerFirstName = "Jan")

        assertEquals(
            "Die offene Anfrage bei Muster, Max wird gelöscht. Trotzdem speichern?",
            requestReplacementWarning(requested, other),
        )
        assertEquals(
            "Die offene Anfrage bei Muster, Max wird gelöscht. Trotzdem speichern?",
            requestReplacementWarning(requested, free),
        )
    }

    @Test
    fun `no warning for the same speaker or without open request`() {
        assertNull(requestReplacementWarning(requested, requested.copy(note = "x")))
        assertNull(requestReplacementWarning(requested, requested.copy(requestId = null)))
        assertNull(requestReplacementWarning(muster, free))
        assertNull(requestReplacementWarning(null, muster))
    }
}
