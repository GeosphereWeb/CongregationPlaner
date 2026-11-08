package de.geosphere.congregationplaner.user.presentation

import de.geosphere.congregationplaner.user.domain.model.EmergencyContact
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MemberProfileEditingTest {
    @Test
    fun withEmailTruncatesToTheMaximumLength() {
        val profile = MemberProfile().withEmail(0, "a".repeat(MemberProfile.MAX_EMAIL_LENGTH + 5))

        assertEquals(MemberProfile.MAX_EMAIL_LENGTH, profile.emails.single().length)
    }

    @Test
    fun withEmailKeepsNoAddressWhenOnlyAnEmptyFirstFieldRemains() {
        val profile = MemberProfile(emails = listOf("a@b.de")).withEmail(0, "")

        assertEquals(emptyList(), profile.emails)
    }

    @Test
    fun addedEmailStopsAtTheMaximum() {
        var profile = MemberProfile(emails = listOf("a@b.de"))
        repeat(MemberProfile.MAX_EMAILS + 2) { profile = profile.withAddedEmail() }

        assertEquals(MemberProfile.MAX_EMAILS, profile.emails.size)
        assertFalse(profile.canAddEmail)
    }

    @Test
    fun firstEmailCannotBeRemoved() {
        val profile = MemberProfile(emails = listOf("a@b.de", "c@d.de"))

        assertSame(profile, profile.withoutEmail(0))
        assertEquals(listOf("a@b.de"), profile.withoutEmail(1).emails)
    }

    @Test
    fun contactsAreAddedReplacedAndRemoved() {
        val profile = MemberProfile().withAddedContact().withContact(0, EmergencyContact(name = "Erika"))

        assertEquals("Erika", profile.emergencyContacts.single().name)
        assertEquals(emptyList(), profile.withoutContact(0).emergencyContacts)
    }

    @Test
    fun addedContactStopsAtTheMaximum() {
        var profile = MemberProfile()
        repeat(MemberProfile.MAX_EMERGENCY_CONTACTS + 2) { profile = profile.withAddedContact() }

        assertEquals(MemberProfile.MAX_EMERGENCY_CONTACTS, profile.emergencyContacts.size)
        assertFalse(profile.canAddContact)
    }

    @Test
    fun contactPhonesStopAtTheMaximumAndKeepTheFirst() {
        var contact = EmergencyContact()
        repeat(MemberProfile.MAX_PHONES_PER_CONTACT + 2) { contact = contact.withAddedPhone() }

        assertEquals(MemberProfile.MAX_PHONES_PER_CONTACT, contact.phones.size)
        assertFalse(contact.canAddPhone)
        assertTrue(contact.canRemovePhone)
    }

    @Test
    fun singleContactPhoneCannotBeRemoved() {
        val contact = EmergencyContact(phones = listOf("0171"))

        assertFalse(contact.canRemovePhone)
        assertSame(contact, contact.withoutPhone(0))
    }

    @Test
    fun contactPhoneIsReplacedAndRemovedByIndex() {
        val contact = EmergencyContact(phones = listOf("1", "2")).withPhone(1, "3")

        assertEquals(listOf("1", "3"), contact.phones)
        assertEquals(listOf("3"), contact.withoutPhone(0).phones)
    }
}
