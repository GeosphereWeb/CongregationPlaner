package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.EmergencyContact
import de.geosphere.congregationplaner.user.domain.model.MemberPrivateDetails
import de.geosphere.congregationplaner.user.domain.model.MemberPrivateException
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.repository.MemberPrivateRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

/** Die eigenen privaten Angaben; ohne gespeicherte Angaben leer, ohne Versammlung gibt es keine. */
@Inject
class ObserveOwnPrivateDetailsUseCase(private val repository: MemberPrivateRepository) {
    operator fun invoke(user: AppUser): Flow<MemberPrivateDetails> = user.congregation?.let { congregation ->
        repository.observe(congregation, user.uid).map { it ?: MemberPrivateDetails() }
    } ?: emptyFlow()
}

/**
 * Die privaten Angaben eines Mitglieds für einen Versammlungsadmin (nur lesen, ADR-0015); anderen liefert der Flow
 * nichts, die Rules würden ohnehin ablehnen.
 */
@Inject
class ObserveMemberPrivateDetailsUseCase(
    private val repository: MemberPrivateRepository,
    private val policy: UserPermissionPolicy,
) {
    operator fun invoke(admin: AppUser, member: AppUser): Flow<MemberPrivateDetails?> {
        val congregation = admin.congregation
        if (congregation == null || !policy.canManageUsers(admin) || !member.hasAccount) return emptyFlow()
        return repository.observe(congregation, member.uid)
    }
}

/**
 * Speichert die eigenen privaten Angaben. Leere Notfallkontakte (ohne Name und Telefon) werden weggelassen, die übrigen
 * brauchen beides; alle Eingaben werden getrimmt und gegen dieselben Grenzen geprüft wie in den Rules.
 */
@Inject
class SaveOwnPrivateDetailsUseCase(private val repository: MemberPrivateRepository) {
    suspend operator fun invoke(user: AppUser, details: MemberPrivateDetails): Result<MemberPrivateDetails> {
        val congregation = user.congregation
            ?: return Result.failure(IllegalStateException("Ohne Versammlung gibt es keine privaten Angaben."))
        val cleaned = details.cleaned()
        cleaned.validationError()?.let { return Result.failure(it) }
        return repository.save(congregation, user.uid, cleaned).map { cleaned }
    }
}

private fun MemberPrivateDetails.cleaned(): MemberPrivateDetails = copy(
    street = street.trim(),
    postalCode = postalCode.trim(),
    city = city.trim(),
    emergencyContacts = emergencyContacts
        .map { EmergencyContact(it.name.trim(), it.relation.trim(), it.phone.trim()) }
        .filterNot { it.name.isEmpty() && it.relation.isEmpty() && it.phone.isEmpty() },
)

private fun MemberPrivateDetails.validationError(): MemberPrivateException? = when {
    street.length > MemberPrivateDetails.MAX_STREET_LENGTH ||
        postalCode.length > MemberPrivateDetails.MAX_POSTAL_CODE_LENGTH ||
        city.length > MemberPrivateDetails.MAX_CITY_LENGTH -> MemberPrivateException.AddressTooLong()
    emergencyContacts.size > MemberPrivateDetails.MAX_EMERGENCY_CONTACTS -> MemberPrivateException.TooManyContacts()
    emergencyContacts.any { it.name.isEmpty() || it.phone.isEmpty() } -> MemberPrivateException.IncompleteContact()
    emergencyContacts.any { !it.isValid() } -> MemberPrivateException.InvalidContact()
    else -> null
}

private fun EmergencyContact.isValid(): Boolean = name.length <= MemberPrivateDetails.MAX_CONTACT_NAME_LENGTH &&
    relation.length <= MemberPrivateDetails.MAX_RELATION_LENGTH &&
    MemberProfile.isValidPhone(phone)
