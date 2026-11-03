package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.model.MemberProfileException
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject

/**
 * Speichert Nachnamen, Vornamen, Handy und Festnetz des angemeldeten Mitglieds im eigenen Mitgliedsdokument. Beide
 * Namen sind Pflicht, die Telefonnummern optional; andere Angaben bleiben bei den Admins.
 */
@Inject
class UpdateOwnProfileUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(
        user: AppUser,
        lastName: String,
        firstName: String,
        mobilePhone: String = user.profile.mobilePhone,
        landlinePhone: String = user.profile.landlinePhone,
    ): Result<AppUser> {
        val congregation = user.congregation
            ?: return Result.failure(IllegalStateException("Ohne Versammlung gibt es kein Mitgliedsdokument."))
        val profile = user.profile.copy(
            lastName = lastName.trim(),
            firstName = firstName.trim(),
            mobilePhone = mobilePhone.trim(),
            landlinePhone = landlinePhone.trim(),
        )
        val error = when {
            profile.lastName.isEmpty() || profile.firstName.isEmpty() -> MemberProfileException.MissingName()
            maxOf(profile.lastName.length, profile.firstName.length) > MemberProfile.MAX_NAME_LENGTH ->
                MemberProfileException.NameTooLong()
            !MemberProfile.isValidPhone(profile.mobilePhone) || !MemberProfile.isValidPhone(profile.landlinePhone) ->
                MemberProfileException.InvalidPhone()
            else -> null
        }
        return error?.let { Result.failure(it) }
            ?: repository.updateOwnProfile(congregation, user.copy(profile = profile))
    }
}
