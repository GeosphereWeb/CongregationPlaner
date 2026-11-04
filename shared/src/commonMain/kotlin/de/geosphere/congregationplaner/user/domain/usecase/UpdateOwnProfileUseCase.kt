package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject

/**
 * Speichert die eigenen Kontaktangaben des angemeldeten Mitglieds (Namen, Telefon, E-Mail, Adresse, Notfallkontakte)
 * aus [form] im eigenen Mitgliedsdokument. Beide Namen sind Pflicht; Kategorie, Taufe, Dienstamt und Redner bleiben,
 * wie der Admin sie gesetzt hat.
 */
@Inject
class UpdateOwnProfileUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(user: AppUser, form: MemberProfile): Result<AppUser> {
        val congregation = user.congregation
            ?: return Result.failure(IllegalStateException("Ohne Versammlung gibt es kein Mitgliedsdokument."))
        return user.profile.withOwnFieldsOf(form).validated(requireName = true).fold(
            onSuccess = { repository.updateOwnProfile(congregation, user.copy(profile = it)) },
            onFailure = { Result.failure(it) },
        )
    }
}
