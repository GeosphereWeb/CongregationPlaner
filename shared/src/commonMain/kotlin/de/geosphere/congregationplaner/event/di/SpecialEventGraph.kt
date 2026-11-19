package de.geosphere.congregationplaner.event.di

import de.geosphere.congregationplaner.event.data.createEventTypeRepository
import de.geosphere.congregationplaner.event.data.createSpecialEventRepository
import de.geosphere.congregationplaner.event.domain.repository.EventTypeRepository
import de.geosphere.congregationplaner.event.domain.repository.SpecialEventRepository
import de.geosphere.congregationplaner.event.presentation.SpecialEventManagementViewModel
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface SpecialEventGraph {
    val specialEventManagementViewModel: SpecialEventManagementViewModel

    @Provides
    fun provideSpecialEventRepository(): SpecialEventRepository = createSpecialEventRepository()

    @Provides
    fun provideEventTypeRepository(): EventTypeRepository = createEventTypeRepository()
}
