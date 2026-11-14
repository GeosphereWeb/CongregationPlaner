package de.geosphere.congregationplaner.organization.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.CountryOption
import de.geosphere.congregationplaner.organization.domain.model.CountryRef
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import dev.zacsweers.metro.Inject

/** All countries of the organization, walked from the regions down and sorted by name. */
@Inject
class ListCountriesUseCase(private val loadUnits: LoadOrganizationUnitsUseCase) {
    suspend operator fun invoke(): Result<List<CountryOption>> = runCatching {
        collect(OrgLevel.REGION, emptyList()).sortedBy { it.displayName.lowercase() }
    }

    private suspend fun collect(level: OrgLevel, parentIds: List<String>): List<CountryOption> {
        val units = loadUnits(level, parentIds).getOrThrow()
        return if (level == OrgLevel.COUNTRY) {
            units.mapNotNull { unit -> CountryRef.fromIds(parentIds + unit.id)?.let { CountryOption(it, unit.name) } }
        } else {
            val child = checkNotNull(level.child)
            units.flatMap { collect(child, parentIds + it.id) }
        }
    }
}
