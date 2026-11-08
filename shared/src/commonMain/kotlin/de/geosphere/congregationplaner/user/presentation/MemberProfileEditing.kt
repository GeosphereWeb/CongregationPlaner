package de.geosphere.congregationplaner.user.presentation

import de.geosphere.congregationplaner.user.domain.model.EmergencyContact
import de.geosphere.congregationplaner.user.domain.model.MemberProfile

/** Das erste E-Mail-Feld ist immer da, auch ohne gespeicherte Adresse; weitere kommen bei Bedarf dazu. */
internal val MemberProfile.emailFields: List<String> get() = emails.ifEmpty { listOf("") }

internal val MemberProfile.canAddEmail: Boolean get() = emailFields.size < MemberProfile.MAX_EMAILS

internal val MemberProfile.canAddContact: Boolean
    get() = emergencyContacts.size < MemberProfile.MAX_EMERGENCY_CONTACTS

/**
 * Setzt die Adresse im Feld [index], auf die erlaubte Länge gekürzt. Bleibt nur ein leeres erstes Feld übrig, gibt es
 * keine Adresse; so ändert sich das Profil nicht unnötig.
 */
internal fun MemberProfile.withEmail(index: Int, value: String): MemberProfile = copy(
    emails = emailFields.replaced(index, value.take(MemberProfile.MAX_EMAIL_LENGTH))
        .takeUnless { it == listOf("") }
        .orEmpty(),
)

internal fun MemberProfile.withAddedEmail(): MemberProfile = if (canAddEmail) copy(emails = emailFields + "") else this

/** Das erste Feld bleibt immer; entfernt werden nur weitere. */
internal fun MemberProfile.withoutEmail(index: Int): MemberProfile =
    if (index == 0) this else copy(emails = emails.filterIndexed { i, _ -> i != index })

internal fun MemberProfile.withContact(index: Int, contact: EmergencyContact): MemberProfile =
    copy(emergencyContacts = emergencyContacts.replaced(index, contact))

internal fun MemberProfile.withAddedContact(): MemberProfile =
    if (canAddContact) copy(emergencyContacts = emergencyContacts + EmergencyContact()) else this

internal fun MemberProfile.withoutContact(index: Int): MemberProfile =
    copy(emergencyContacts = emergencyContacts.filterIndexed { i, _ -> i != index })

internal val EmergencyContact.canAddPhone: Boolean get() = phones.size < MemberProfile.MAX_PHONES_PER_CONTACT

/** Die erste Nummer bleibt immer; entfernt werden nur weitere. */
internal val EmergencyContact.canRemovePhone: Boolean get() = phones.size > 1

internal fun EmergencyContact.withPhone(index: Int, value: String): EmergencyContact =
    copy(phones = phones.replaced(index, value))

internal fun EmergencyContact.withAddedPhone(): EmergencyContact = if (canAddPhone) copy(phones = phones + "") else this

internal fun EmergencyContact.withoutPhone(index: Int): EmergencyContact =
    if (canRemovePhone) copy(phones = phones.filterIndexed { i, _ -> i != index }) else this

private fun <T> List<T>.replaced(index: Int, value: T): List<T> =
    mapIndexed { i, old -> if (i == index) value else old }
