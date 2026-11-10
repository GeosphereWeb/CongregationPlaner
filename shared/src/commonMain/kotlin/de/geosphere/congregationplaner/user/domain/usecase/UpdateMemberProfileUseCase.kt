package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.Appointment
import de.geosphere.congregationplaner.user.domain.model.EmergencyContact
import de.geosphere.congregationplaner.user.domain.model.MemberCategory
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.model.MemberProfileException
import de.geosphere.congregationplaner.user.domain.model.formatPhoneNumber
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject

/** Der Versammlungsadmin [user] speichert alle persönlichen Angaben von [member]. */
@Inject
class UpdateMemberProfileUseCase(private val repository: UserRepository, private val policy: UserPermissionPolicy) {
    suspend operator fun invoke(user: AppUser, member: AppUser): Result<AppUser> {
        if (member.uid.isBlank()) return Result.failure(IllegalArgumentException("Das Mitglied hat keine ID."))
        val congregation = user.congregation
        if (congregation == null || !policy.canEditProfile(user, member)) {
            return Result.failure(MemberProfileException.NotAllowed())
        }
        // Bei Personen ohne Konto ist der Name die einzige Möglichkeit, sie zu erkennen.
        return member.profile.validated(requireName = !member.hasAccount).fold(
            onSuccess = { profile -> repository.updateMemberProfile(congregation, member.copy(profile = profile)) },
            onFailure = { Result.failure(it) },
        )
    }
}

/**
 * Trimmt die Eingaben, formatiert die Telefonnummern, lässt leere E-Mail-Felder und leere Notfallkontakte weg und prüft
 * alles gegen dieselben Grenzen wie die Firestore Security Rules (die Nummern der Notfallkontakte prüft nur die App).
 */
internal fun MemberProfile.validated(requireName: Boolean): Result<MemberProfile> {
    val cleaned = cleaned()
    val error = when {
        requireName && (cleaned.lastName.isEmpty() || cleaned.firstName.isEmpty()) ->
            MemberProfileException.MissingName()
        maxOf(cleaned.lastName.length, cleaned.firstName.length) > MemberProfile.MAX_NAME_LENGTH ->
            MemberProfileException.NameTooLong()
        cleaned.category == MemberCategory.CHILD && cleaned.baptized -> MemberProfileException.BaptizedChild()
        cleaned.appointment != Appointment.NONE && !cleaned.canHaveAppointment ->
            MemberProfileException.AppointmentNotAllowed()
        cleaned.isSpeaker && !cleaned.canBeSpeaker -> MemberProfileException.SpeakerNotAllowed()
        !MemberProfile.isValidPhone(cleaned.mobilePhone) || !MemberProfile.isValidPhone(cleaned.landlinePhone) ->
            MemberProfileException.InvalidPhone()
        cleaned.emails.size > MemberProfile.MAX_EMAILS -> MemberProfileException.TooManyEmails()
        !cleaned.emails.all(MemberProfile::isValidEmail) -> MemberProfileException.InvalidEmail()
        else -> cleaned.addressError()
    }
    return error?.let { Result.failure(it) } ?: Result.success(cleaned)
}

private fun MemberProfile.cleaned(): MemberProfile = copy(
    lastName = lastName.trim(),
    firstName = firstName.trim(),
    mobilePhone = formatPhoneNumber(mobilePhone),
    landlinePhone = formatPhoneNumber(landlinePhone),
    emails = emails.map(String::trim).filter(String::isNotEmpty),
    street = street.trim(),
    postalCode = postalCode.trim(),
    city = city.trim(),
    emergencyContacts = emergencyContacts
        .map { contact ->
            EmergencyContact(
                name = contact.name.trim(),
                relation = contact.relation.trim(),
                phones = contact.phones.map(::formatPhoneNumber).filter(String::isNotEmpty),
            )
        }
        .filterNot { it.name.isEmpty() && it.relation.isEmpty() && it.phones.isEmpty() },
)

private fun MemberProfile.addressError(): MemberProfileException? = when {
    street.length > MemberProfile.MAX_STREET_LENGTH ||
        postalCode.length > MemberProfile.MAX_POSTAL_CODE_LENGTH ||
        city.length > MemberProfile.MAX_CITY_LENGTH -> MemberProfileException.AddressTooLong()
    emergencyContacts.size > MemberProfile.MAX_EMERGENCY_CONTACTS -> MemberProfileException.TooManyContacts()
    emergencyContacts.any { it.name.isEmpty() || it.phones.isEmpty() } -> MemberProfileException.IncompleteContact()
    emergencyContacts.any { it.phones.size > MemberProfile.MAX_PHONES_PER_CONTACT } ->
        MemberProfileException.TooManyPhones()
    emergencyContacts.any { !it.isValid() } -> MemberProfileException.InvalidContact()
    else -> null
}

private fun EmergencyContact.isValid(): Boolean = name.length <= MemberProfile.MAX_CONTACT_NAME_LENGTH &&
    relation.length <= MemberProfile.MAX_RELATION_LENGTH &&
    phones.all(MemberProfile::isValidPhone)
