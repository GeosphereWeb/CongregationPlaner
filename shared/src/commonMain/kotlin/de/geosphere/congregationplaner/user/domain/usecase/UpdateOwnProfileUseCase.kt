package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.model.MemberProfileException
import de.geosphere.congregationplaner.user.domain.model.formatPhoneNumber
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject

/**
 * Speichert Nachnamen, Vornamen, Handy, Festnetz und zusätzliche E-Mail-Adressen des angemeldeten Mitglieds im eigenen
 * Mitgliedsdokument. Beide Namen sind Pflicht, der Rest optional; andere Angaben bleiben bei den Admins.
 */
@Inject
class UpdateOwnProfileUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(
        user: AppUser,
        lastName: String,
        firstName: String,
        mobilePhone: String = user.profile.mobilePhone,
        landlinePhone: String = user.profile.landlinePhone,
        emails: List<String> = user.profile.emails,
    ): Result<AppUser> {
        val congregation = user.congregation
            ?: return Result.failure(IllegalStateException("Ohne Versammlung gibt es kein Mitgliedsdokument."))
        val profile = user.profile.copy(
            lastName = lastName.trim(),
            firstName = firstName.trim(),
            mobilePhone = formatPhoneNumber(mobilePhone),
            landlinePhone = formatPhoneNumber(landlinePhone),
            emails = emails.map(String::trim).filter(String::isNotEmpty),
        )
        val error = when {
            profile.lastName.isEmpty() || profile.firstName.isEmpty() -> MemberProfileException.MissingName()
            maxOf(profile.lastName.length, profile.firstName.length) > MemberProfile.MAX_NAME_LENGTH ->
                MemberProfileException.NameTooLong()
            !MemberProfile.isValidPhone(profile.mobilePhone) || !MemberProfile.isValidPhone(profile.landlinePhone) ->
                MemberProfileException.InvalidPhone()
            profile.emails.size > MemberProfile.MAX_EMAILS -> MemberProfileException.TooManyEmails()
            !profile.emails.all(MemberProfile::isValidEmail) -> MemberProfileException.InvalidEmail()
            else -> null
        }
        return error?.let { Result.failure(it) }
            ?: repository.updateOwnProfile(congregation, user.copy(profile = profile))
    }
}
