package de.geosphere.congregationplaner.organization.di

import de.geosphere.congregationplaner.organization.data.createOrgUnitRepository
import de.geosphere.congregationplaner.organization.domain.repository.OrgUnitRepository
import de.geosphere.congregationplaner.organization.presentation.OrganizationViewModel
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

/** Der Bereich „Organisation“ der Globalen Verwaltung. */
@DependencyGraph
interface OrganizationManagementGraph {
    val organizationViewModel: OrganizationViewModel

    @Provides
    fun provideOrgUnitRepository(): OrgUnitRepository = createOrgUnitRepository()
}
