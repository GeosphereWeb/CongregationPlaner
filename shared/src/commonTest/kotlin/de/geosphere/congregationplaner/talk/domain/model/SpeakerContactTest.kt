package de.geosphere.congregationplaner.talk.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpeakerContactTest {
    @Test
    fun `a mobile number is called and reachable by whatsapp`() {
        val contact = SpeakerContact(mobilePhone = "0170 1234567", landlinePhone = "089 123456")

        assertEquals("tel:+491701234567", contact.callUri)
        assertEquals("https://wa.me/491701234567", contact.whatsAppUri)
    }

    @Test
    fun `without a mobile number the landline is called and there is no whatsapp`() {
        val contact = SpeakerContact(landlinePhone = "089 123456")

        assertEquals("tel:+4989123456", contact.callUri)
        assertNull(contact.whatsAppUri)
    }

    @Test
    fun `an unusable mobile number falls back to the landline`() {
        val contact = SpeakerContact(mobilePhone = "12345", landlinePhone = "+49 89 123456")

        assertEquals("tel:+4989123456", contact.callUri)
        assertNull(contact.whatsAppUri)
    }

    @Test
    fun `a foreign mobile number works with whatsapp`() {
        val contact = SpeakerContact(mobilePhone = "+43 660 1234567")

        assertEquals("tel:+436601234567", contact.callUri)
        assertEquals("https://wa.me/436601234567", contact.whatsAppUri)
    }

    @Test
    fun `no numbers mean no actions`() {
        assertNull(SpeakerContact().callUri)
        assertNull(SpeakerContact().whatsAppUri)
    }
}
