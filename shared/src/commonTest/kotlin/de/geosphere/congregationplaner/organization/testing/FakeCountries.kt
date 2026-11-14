package de.geosphere.congregationplaner.organization.testing

import de.geosphere.congregationplaner.organization.domain.model.CountryOption
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.repository.OrganizationRepository
import de.geosphere.congregationplaner.organization.domain.usecase.ListCountriesUseCase
import de.geosphere.congregationplaner.organization.domain.usecase.LoadOrganizationUnitsUseCase
import de.geosphere.congregationplaner.user.testing.testCongregation

/** Germany, the country of [testCongregation]. */
internal val testCountry = CountryOption(testCongregation.country, "Deutschland")

/** An [OrganizationRepository] holding exactly [countries] down to the country level; [failure] fails every read. */
internal class FakeCountryRepository(private val countries: List<CountryOption>, var failure: Throwable? = null) :
    OrganizationRepository {
    var reads = 0

    override suspend fun listUnits(level: OrgLevel, parentIds: List<String>): Result<List<OrgUnit>> {
        reads++
        failure?.let { return Result.failure(it) }
        val matching = countries.filter { it.ref.ids.take(parentIds.size) == parentIds }
        val units = if (level == OrgLevel.COUNTRY) {
            matching.map { OrgUnit(it.ref.countryId, it.name) }
        } else {
            matching.map { it.ref.ids[level.ordinal] }.distinct().map { OrgUnit(it) }
        }
        return Result.success(units)
    }
}

internal fun listCountries(repository: OrganizationRepository) =
    ListCountriesUseCase(LoadOrganizationUnitsUseCase(repository))

internal fun listCountries(vararg countries: CountryOption) = listCountries(FakeCountryRepository(countries.toList()))
