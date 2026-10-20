package de.geosphere.congregationplaner.organization.di

import de.geosphere.congregationplaner.organization.data.createCongregationSelectionStore
import de.geosphere.congregationplaner.organization.data.createOrganizationRepository
import de.geosphere.congregationplaner.organization.domain.repository.CongregationSelectionStore
import de.geosphere.congregationplaner.organization.domain.repository.OrganizationRepository
import de.geosphere.congregationplaner.organization.presentation.CongregationSelectionViewModel
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface OrganizationGraph {
    val congregationSelectionViewModel: CongregationSelectionViewModel

    @Provides
    fun provideOrganizationRepository(): OrganizationRepository = createOrganizationRepository()

    @Provides
    fun provideCongregationSelectionStore(): CongregationSelectionStore = createCongregationSelectionStore()
}
