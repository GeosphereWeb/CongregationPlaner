package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Inject
class ObserveWeekEventsUseCase(private val repository: WeekEventRepository) {
    operator fun invoke(): Flow<List<WeekEvent>> =
        repository.observeWeekEvents().map { events -> events.sortedBy { it.name.lowercase() } }
}
