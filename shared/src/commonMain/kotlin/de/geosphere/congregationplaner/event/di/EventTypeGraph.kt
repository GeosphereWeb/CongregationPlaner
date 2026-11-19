package de.geosphere.congregationplaner.event.di

import de.geosphere.congregationplaner.event.data.createEventTypeRepository
import de.geosphere.congregationplaner.event.domain.repository.EventTypeRepository
import de.geosphere.congregationplaner.event.presentation.EventTypeManagementViewModel
import de.geosphere.congregationplaner.talk.data.createWeekEventRepository
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface EventTypeGraph {
    val eventTypeManagementViewModel: EventTypeManagementViewModel

    @Provides
    fun provideEventTypeRepository(): EventTypeRepository = createEventTypeRepository()

    /** Die Wochenereignisse des Landes sind die Vorlage (ADR-0036). */
    @Provides
    fun provideWeekEventRepository(): WeekEventRepository = createWeekEventRepository()
}
