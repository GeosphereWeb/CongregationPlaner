package de.geosphere.congregationplaner.talk.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpeakerContactTest {
    @Test
    fun `mobile and landline each get their own call link and whatsapp uses the mobile number`() {
        val contact = SpeakerContact(mobilePhone = "0170 1234567", landlinePhone = "089 123456")

        assertEquals("tel:+491701234567", contact.mobileCallUri)
        assertEquals("tel:+4989123456", contact.landlineCallUri)
        assertEquals("https://wa.me/491701234567", contact.whatsAppUri)
        assertTrue(contact.isReachable)
    }

    @Test
    fun `with only a landline there is no mobile call and no whatsapp`() {
        val contact = SpeakerContact(landlinePhone = "089 123456")

        assertNull(contact.mobileCallUri)
        assertEquals("tel:+4989123456", contact.landlineCallUri)
        assertNull(contact.whatsAppUri)
        assertTrue(contact.isReachable)
    }

    @Test
    fun `with only a mobile number there is no landline call`() {
        val contact = SpeakerContact(mobilePhone = "0170 1234567")

        assertEquals("tel:+491701234567", contact.mobileCallUri)
        assertNull(contact.landlineCallUri)
        assertTrue(contact.isReachable)
    }

    @Test
    fun `an unusable number counts as missing`() {
        val contact = SpeakerContact(mobilePhone = "12345", landlinePhone = "+49 89 123456")

        assertNull(contact.mobileCallUri)
        assertNull(contact.whatsAppUri)
        assertEquals("tel:+4989123456", contact.landlineCallUri)
    }

    @Test
    fun `a foreign mobile number works with whatsapp`() {
        val contact = SpeakerContact(mobilePhone = "+43 660 1234567")

        assertEquals("tel:+436601234567", contact.mobileCallUri)
        assertEquals("https://wa.me/436601234567", contact.whatsAppUri)
    }

    @Test
    fun `no numbers mean no actions`() {
        val contact = SpeakerContact()

        assertNull(contact.mobileCallUri)
        assertNull(contact.landlineCallUri)
        assertNull(contact.whatsAppUri)
        assertFalse(contact.isReachable)
    }
}
