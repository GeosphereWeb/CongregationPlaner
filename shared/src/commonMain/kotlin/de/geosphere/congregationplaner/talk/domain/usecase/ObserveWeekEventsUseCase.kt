package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.CountryRef
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Inject
class ObserveWeekEventsUseCase(private val repository: WeekEventRepository) {
    operator fun invoke(country: CountryRef): Flow<List<WeekEvent>> =
        repository.observeWeekEvents(country).map { events -> events.sortedBy { it.name.lowercase() } }

    /** Die Ereignisse des Landes der eigenen Versammlung; ohne Versammlung leer. */
    operator fun invoke(user: AppUser): Flow<List<WeekEvent>> =
        user.congregation?.let { invoke(it.country) } ?: flowOf(emptyList())
}
