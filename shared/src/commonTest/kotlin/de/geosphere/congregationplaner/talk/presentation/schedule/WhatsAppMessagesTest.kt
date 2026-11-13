package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.SpeakerContact
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.user.domain.model.DEFAULT_MESSAGE_TEMPLATES
import de.geosphere.congregationplaner.user.domain.model.MessagePlaceholder
import de.geosphere.congregationplaner.user.domain.model.MessageTemplate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WhatsAppMessagesTest {
    private val slot = TalkSlot(
        LocalDate(2026, 11, 15),
        LocalTime(9, 5),
        talkNumber = 12,
        talkTitle = "Der Weg zum Glück",
        externalSpeakerId = "e1",
        speakerLastName = "Extern",
        speakerFirstName = "Eva",
        speakerCongregation = "Nord",
    )
    private val sender = MessageSender("Paul", "Planer")
    private val contact = SpeakerContact(mobilePhone = "0170 1234567")

    @Test
    fun `every placeholder is filled from slot speaker and sender`() {
        val text = MessagePlaceholder.entries.joinToString("|") { it.key }

        assertEquals(
            "Eva|Extern|Sonntag|15.11.2026|09:05|Nr. 12 – Der Weg zum Glück|12|Der Weg zum Glück|Paul|Planer",
            fillTemplate(text, slotMessageValues(slot, sender)),
        )
    }

    @Test
    fun `unknown placeholders stay and a free talk has no number`() {
        val free = slot.copy(talkNumber = null, talkTitle = "Sondervortrag")
        val values = slotMessageValues(free, sender)

        assertEquals("{Gruß} Sondervortrag ()", fillTemplate("{Gruß} {Vortrag} ({Vortragsnummer})", values))
    }

    @Test
    fun `messages follow the template order and carry the encoded text`() {
        val templates = listOf(MessageTemplate("Kurz", "Hallo {Vorname}!"), MessageTemplate("Dank", "Danke & tschüß"))

        val messages = slotWhatsAppMessages(slot, contact, templates, sender)

        assertEquals(listOf("Kurz", "Dank"), messages.map { it.name })
        assertEquals("https://wa.me/491701234567?text=Hallo%20Eva%21", messages[0].uri)
        assertEquals("https://wa.me/491701234567?text=Danke%20%26%20tsch%C3%BC%C3%9F", messages[1].uri)
    }

    @Test
    fun `no messages without a mobile number`() {
        assertTrue(
            slotWhatsAppMessages(
                slot,
                SpeakerContact(landlinePhone = "089 123456"),
                DEFAULT_MESSAGE_TEMPLATES,
                sender,
            ).isEmpty(),
        )
        assertTrue(slotWhatsAppMessages(slot, null, DEFAULT_MESSAGE_TEMPLATES, sender).isEmpty())
    }
}
