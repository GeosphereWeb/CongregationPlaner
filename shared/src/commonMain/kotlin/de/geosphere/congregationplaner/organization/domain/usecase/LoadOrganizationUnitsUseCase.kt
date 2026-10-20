package de.geosphere.congregationplaner.organization.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.repository.OrganizationRepository
import dev.zacsweers.metro.Inject

@Inject
class LoadOrganizationUnitsUseCase(private val repository: OrganizationRepository) {
    suspend operator fun invoke(level: OrgLevel, parentIds: List<String>): Result<List<OrgUnit>> =
        repository.listUnits(level, parentIds).map { units -> units.sortedBy { it.displayName.lowercase() } }
}
