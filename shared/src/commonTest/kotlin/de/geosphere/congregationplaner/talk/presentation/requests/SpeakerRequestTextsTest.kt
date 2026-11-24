package de.geosphere.congregationplaner.talk.presentation.requests

import de.geosphere.congregationplaner.talk.domain.model.ContactAttempt
import de.geosphere.congregationplaner.talk.domain.model.ContactKind
import de.geosphere.congregationplaner.talk.domain.model.OpenRequest
import de.geosphere.congregationplaner.talk.domain.model.SpeakerRequest
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.assignableSlots
import de.geosphere.congregationplaner.ui.list.CardHighlight
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class SpeakerRequestTextsTest {
    private val today = LocalDate(2026, 11, 24)
    private val slot = TalkSlot(LocalDate(2026, 12, 6), LocalTime(10, 0), speakerLastName = "Gast", requestId = "r1")
    private val request = SpeakerRequest(
        "r1",
        LocalDate(2026, 11, 10),
        contactAttempts = listOf(ContactAttempt(LocalDate(2026, 11, 12), ContactKind.VOICEMAIL)),
    )

    @Test
    fun `the day line shows the date or an open day or a passed date`() {
        assertEquals("So, 06.12.2026", requestDayText(OpenRequest(request, slot, false, false)))
        val undated = OpenRequest(request.copy(speakerLastName = "Gast"), null, false, false)
        assertEquals("Tag offen", requestDayText(undated))
        assertEquals(
            "So, 01.11.2026 · Termin verstrichen",
            requestDayText(OpenRequest(request, slot.copy(date = LocalDate(2026, 11, 1)), false, true)),
        )
    }

    @Test
    fun `the status line shows since when and the last contact`() {
        assertEquals("seit 10.11. · Anrufbeantworter 12.11.", requestStatusText(request))
        assertEquals("seit 10.11. · noch kein Kontakt", requestStatusText(request.copy(contactAttempts = emptyList())))
    }

    @Test
    fun `a draft becomes an undated request`() {
        val draft = TalkSlot(
            today,
            LocalTime(0, 0),
            talkNumber = 12,
            talkTitle = "Zwölf",
            externalSpeakerId = "e1",
            speakerLastName = "Extern",
            speakerFirstName = "Eva",
            speakerCongregation = "Nord",
        )

        assertEquals(
            SpeakerRequest(
                id = "",
                createdOn = today,
                note = "Frühjahr",
                externalSpeakerId = "e1",
                speakerLastName = "Extern",
                speakerFirstName = "Eva",
                speakerCongregation = "Nord",
                talkNumber = 12,
                talkTitle = "Zwölf",
            ),
            requestFromDraft(draft, "Frühjahr", today),
        )
    }

    @Test
    fun `follow up and passed days are highlighted`() {
        assertEquals(CardHighlight.Warning, requestHighlight(OpenRequest(request, slot, true, false)))
        assertEquals(CardHighlight.Warning, requestHighlight(OpenRequest(request, slot, false, true)))
        assertEquals(CardHighlight.None, requestHighlight(OpenRequest(request, slot, false, false)))
    }

    @Test
    fun `the decline question names speaker and day`() {
        assertEquals(
            "Die Anfrage bei Gast wird mit allen Kontaktversuchen gelöscht. Der Tag So, 06.12.2026 wird wieder frei.",
            declineQuestion(OpenRequest(request, slot, false, false)),
        )
        val undated = OpenRequest(request.copy(speakerLastName = "Huber", speakerFirstName = ""), null, false, false)
        assertEquals("Die Anfrage bei Huber wird mit allen Kontaktversuchen gelöscht.", declineQuestion(undated))
    }

    @Test
    fun `an attempt note is cut to its limit`() {
        assertEquals(SpeakerRequest.MAX_ATTEMPT_NOTE_LENGTH, limitAttemptNote("x".repeat(250)).length)
        assertEquals("kurz", limitAttemptNote("kurz"))
    }

    @Test
    fun `only free future days with a talk can be assigned`() {
        val free = TalkSlot(LocalDate(2026, 12, 13), LocalTime(10, 0))
        val slots = listOf(
            free,
            TalkSlot(LocalDate(2026, 11, 1), LocalTime(10, 0)),
            TalkSlot(LocalDate(2026, 12, 20), LocalTime(10, 0), talkCancelled = true),
            slot,
            TalkSlot(LocalDate(2026, 11, 29), LocalTime(10, 0)),
        )

        assertEquals(
            listOf(LocalDate(2026, 11, 29), LocalDate(2026, 12, 13)),
            assignableSlots(slots, today).map { it.date },
        )
    }
}
