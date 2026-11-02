package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.model.MemberProfileException
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject

/**
 * Speichert Nachnamen und Vornamen des angemeldeten Mitglieds im eigenen Mitgliedsdokument. Beide Namen sind
 * Pflicht; andere Angaben bleiben bei den Admins.
 */
@Inject
class UpdateOwnNameUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(user: AppUser, lastName: String, firstName: String): Result<AppUser> {
        val congregation = user.congregation
            ?: return Result.failure(IllegalStateException("Ohne Versammlung gibt es kein Mitgliedsdokument."))
        val last = lastName.trim()
        val first = firstName.trim()
        if (last.isEmpty() || first.isEmpty()) return Result.failure(MemberProfileException.MissingName())
        if (maxOf(last.length, first.length) > MemberProfile.MAX_NAME_LENGTH) {
            return Result.failure(MemberProfileException.NameTooLong())
        }
        return repository.updateOwnName(
            congregation,
            user.copy(profile = user.profile.copy(lastName = last, firstName = first)),
        )
    }
}
