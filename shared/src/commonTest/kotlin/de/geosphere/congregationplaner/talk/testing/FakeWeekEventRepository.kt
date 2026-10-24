package de.geosphere.congregationplaner.talk.testing

import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeWeekEventRepository(events: List<WeekEvent> = emptyList()) : WeekEventRepository {
    val events = MutableStateFlow(events)

    override fun observeWeekEvents(): Flow<List<WeekEvent>> = events
}
