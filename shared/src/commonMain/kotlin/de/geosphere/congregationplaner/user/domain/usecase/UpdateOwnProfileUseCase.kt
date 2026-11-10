package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject

/**
 * Speichert die eigenen Kontaktangaben des angemeldeten Mitglieds (Namen, Telefon, E-Mail, Adresse, Notfallkontakte)
 * aus [form] im eigenen Mitgliedsdokument. Beide Namen sind Pflicht; Kategorie, Taufe, Dienstamt und Redner bleiben,
 * wie der Admin sie gesetzt hat. Ein globaler Admin ohne Mitgliedsdokument speichert Namen, Telefon und E-Mail-Adressen
 * stattdessen in seinem Admin-Dokument.
 */
@Inject
class UpdateOwnProfileUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(user: AppUser, form: MemberProfile): Result<AppUser> {
        if (user.isGlobalAdmin && !user.isMember) return saveGlobalAdminProfile(user, form)
        val congregation = user.congregation
            ?: return Result.failure(IllegalStateException("Ohne Versammlung gibt es kein Mitgliedsdokument."))
        if (!user.isMember) {
            return Result.failure(IllegalStateException("Als globaler Admin hast du kein Mitgliedsdokument."))
        }
        return user.profile.withOwnFieldsOf(form).validated(requireName = true).fold(
            onSuccess = { repository.updateOwnProfile(congregation, user.copy(profile = it)) },
            onFailure = { Result.failure(it) },
        )
    }

    private suspend fun saveGlobalAdminProfile(user: AppUser, form: MemberProfile): Result<AppUser> =
        user.profile.withOwnFieldsOf(form).validated(requireName = true).fold(
            onSuccess = { profile ->
                repository.updateGlobalAdminProfile(user.uid, profile).map { user.copy(profile = profile) }
            },
            onFailure = { Result.failure(it) },
        )
}
