package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject

/**
 * Stellt Mitgliedsdokumente im alten Format (`role`/`permissions`) auf die Rollenliste um (ADR-0012). Das Ergebnis ist
 * die Anzahl der umgestellten Dokumente; der Aufruf ist idempotent.
 */
@Inject
class MigrateLegacyMemberRolesUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(congregation: CongregationRef): Result<Int> =
        repository.migrateLegacyMembers(congregation)
}
