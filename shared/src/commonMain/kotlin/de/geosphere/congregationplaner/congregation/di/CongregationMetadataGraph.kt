package de.geosphere.congregationplaner.congregation.di

import de.geosphere.congregationplaner.congregation.data.createCongregationMetadataRepository
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.congregation.presentation.CongregationMetadataViewModel
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface CongregationMetadataGraph {
    val congregationMetadataViewModel: CongregationMetadataViewModel

    @Provides
    fun provideCongregationMetadataRepository(): CongregationMetadataRepository = createCongregationMetadataRepository()
}
