package de.geosphere.congregationplaner.organization.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.repository.OrgUnitRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Die Einheiten einer Ebene, nach Namen sortiert. */
@Inject
class ObserveOrgUnitsUseCase(private val repository: OrgUnitRepository) {
    operator fun invoke(level: OrgLevel, parentIds: List<String>): Flow<List<OrgUnit>> {
        require(level.isOrgUnit) { "$level is no organization unit." }
        return repository.observe(level, parentIds).map { units -> units.sortedBy { it.displayName.lowercase() } }
    }
}
