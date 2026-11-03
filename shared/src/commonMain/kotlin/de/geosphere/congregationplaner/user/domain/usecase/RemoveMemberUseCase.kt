package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.repository.MemberPrivateRepository
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject

/**
 * Entfernt ein Mitglied samt seinen privaten Angaben (ADR-0015). Die privaten Angaben werden zuerst gelöscht, damit
 * nach einem Fehler kein Rest ohne Mitglied zurückbleibt.
 */
@Inject
class RemoveMemberUseCase(
    private val repository: UserRepository,
    private val privateDetails: MemberPrivateRepository,
) {
    suspend operator fun invoke(congregation: CongregationRef, uid: String): Result<Unit> {
        if (uid.isBlank()) return Result.failure(IllegalArgumentException("Das Mitglied hat keine ID."))
        privateDetails.delete(congregation, uid).onFailure { return Result.failure(it) }
        return repository.removeMember(congregation, uid)
    }
}
