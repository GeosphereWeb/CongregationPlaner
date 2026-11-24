package de.geosphere.congregationplaner.talk.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpeakerRequestTest {
    private val today = LocalDate(2026, 11, 24)

    private fun request(id: String, createdOn: LocalDate, vararg attempts: LocalDate, speaker: String? = null) =
        SpeakerRequest(
            id = id,
            createdOn = createdOn,
            contactAttempts = attempts.map { ContactAttempt(it, ContactKind.VOICEMAIL) },
            speakerLastName = speaker,
        )

    private fun slot(date: LocalDate, requestId: String) =
        TalkSlot(date, LocalTime(10, 0), speakerLastName = "Muster", requestId = requestId)

    @Test
    fun `last contact is the latest attempt or the creation day`() {
        assertEquals(LocalDate(2026, 11, 1), request("a", LocalDate(2026, 11, 1)).lastContact)
        val withAttempts = request("a", LocalDate(2026, 11, 1), LocalDate(2026, 11, 20), LocalDate(2026, 11, 12))
        assertEquals(LocalDate(2026, 11, 20), withAttempts.lastContact)
    }

    @Test
    fun `open requests join their slot and sort by last contact`() {
        val dated = request("a", LocalDate(2026, 11, 10))
        val undated = request("b", LocalDate(2026, 11, 20), speaker = "Huber")
        val open = openRequestsOf(listOf(undated, dated), listOf(slot(LocalDate(2026, 12, 14), "a")), today)
        assertEquals(listOf("a", "b"), open.map { it.request.id })
        assertEquals(LocalDate(2026, 12, 14), open[0].slot?.date)
        assertNull(open[1].slot)
    }

    @Test
    fun `follow up is needed after more than seven days without contact`() {
        val open = openRequestsOf(
            listOf(
                request("recent", LocalDate(2026, 11, 17), speaker = "A"),
                request("old", LocalDate(2026, 11, 16), speaker = "B"),
            ),
            emptyList(),
            today,
        ).associateBy { it.request.id }
        assertFalse(open.getValue("recent").needsFollowUp)
        assertTrue(open.getValue("old").needsFollowUp)
    }

    @Test
    fun `a request on a past day is marked as passed`() {
        val open = openRequestsOf(listOf(request("a", today)), listOf(slot(LocalDate(2026, 11, 1), "a")), today)
        assertTrue(open.single().datePassed)
    }

    @Test
    fun `an orphan without slot and speaker is hidden`() {
        assertTrue(openRequestsOf(listOf(request("a", today)), emptyList(), today).isEmpty())
    }

    @Test
    fun `a slot in the next year is included`() {
        val open = openRequestsOf(listOf(request("a", today)), listOf(slot(LocalDate(2027, 1, 10), "a")), today)
        assertEquals(LocalDate(2027, 1, 10), open.single().slot?.date)
    }

    @Test
    fun `the speaker of the slot is shown before the speaker of the request`() {
        val open = openRequestsOf(listOf(request("a", today)), listOf(slot(LocalDate(2026, 12, 6), "a")), today)
        assertEquals("Muster", open.single().speakerDisplayName)
    }

    @Test
    fun `new request ids have twenty alphanumeric characters`() {
        val id = newRequestId()
        assertEquals(20, id.length)
        assertTrue(id.all { it.isLetterOrDigit() && it.code < 128 })
    }
}
