package de.geosphere.congregationplaner.user.presentation.contact

import de.geosphere.congregationplaner.user.domain.model.MemberProfile

/** Kürzt die Eingaben auf die Längen, die das Speichern erlaubt. */
internal fun MemberProfile.withinInputLimits(): MemberProfile = copy(
    lastName = lastName.take(MemberProfile.MAX_NAME_LENGTH),
    firstName = firstName.take(MemberProfile.MAX_NAME_LENGTH),
    mobilePhone = mobilePhone.take(MemberProfile.MAX_PHONE_LENGTH),
    landlinePhone = landlinePhone.take(MemberProfile.MAX_PHONE_LENGTH),
    street = street.take(MemberProfile.MAX_STREET_LENGTH),
    postalCode = postalCode.take(MemberProfile.MAX_POSTAL_CODE_LENGTH),
    city = city.take(MemberProfile.MAX_CITY_LENGTH),
    emergencyContacts = emergencyContacts.map { contact ->
        contact.copy(
            name = contact.name.take(MemberProfile.MAX_CONTACT_NAME_LENGTH),
            relation = contact.relation.take(MemberProfile.MAX_RELATION_LENGTH),
            phones = contact.phones.map { it.take(MemberProfile.MAX_PHONE_LENGTH) },
        )
    },
)
