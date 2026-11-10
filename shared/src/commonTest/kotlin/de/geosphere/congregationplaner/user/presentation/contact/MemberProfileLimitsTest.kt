package de.geosphere.congregationplaner.user.presentation.contact

import de.geosphere.congregationplaner.user.domain.model.EmergencyContact
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import kotlin.test.Test
import kotlin.test.assertEquals

class MemberProfileLimitsTest {
    private fun tooLong(limit: Int) = "x".repeat(limit + 3)

    @Test
    fun `every input is cut to the length that may be saved`() {
        val profile = MemberProfile(
            lastName = tooLong(MemberProfile.MAX_NAME_LENGTH),
            firstName = tooLong(MemberProfile.MAX_NAME_LENGTH),
            mobilePhone = tooLong(MemberProfile.MAX_PHONE_LENGTH),
            landlinePhone = tooLong(MemberProfile.MAX_PHONE_LENGTH),
            street = tooLong(MemberProfile.MAX_STREET_LENGTH),
            postalCode = tooLong(MemberProfile.MAX_POSTAL_CODE_LENGTH),
            city = tooLong(MemberProfile.MAX_CITY_LENGTH),
            emergencyContacts = listOf(
                EmergencyContact(
                    tooLong(MemberProfile.MAX_CONTACT_NAME_LENGTH),
                    tooLong(MemberProfile.MAX_RELATION_LENGTH),
                    listOf(tooLong(MemberProfile.MAX_PHONE_LENGTH)),
                ),
            ),
        )

        val limited = profile.withinInputLimits()

        assertEquals(MemberProfile.MAX_NAME_LENGTH, limited.lastName.length)
        assertEquals(MemberProfile.MAX_NAME_LENGTH, limited.firstName.length)
        assertEquals(MemberProfile.MAX_PHONE_LENGTH, limited.mobilePhone.length)
        assertEquals(MemberProfile.MAX_PHONE_LENGTH, limited.landlinePhone.length)
        assertEquals(MemberProfile.MAX_STREET_LENGTH, limited.street.length)
        assertEquals(MemberProfile.MAX_POSTAL_CODE_LENGTH, limited.postalCode.length)
        assertEquals(MemberProfile.MAX_CITY_LENGTH, limited.city.length)
        val contact = limited.emergencyContacts.single()
        assertEquals(MemberProfile.MAX_CONTACT_NAME_LENGTH, contact.name.length)
        assertEquals(MemberProfile.MAX_RELATION_LENGTH, contact.relation.length)
        assertEquals(MemberProfile.MAX_PHONE_LENGTH, contact.phones.single().length)
    }

    @Test
    fun `short inputs stay unchanged`() {
        val profile = MemberProfile(lastName = "Muster", firstName = "Max", city = "Berlin")

        assertEquals(profile, profile.withinInputLimits())
    }
}
