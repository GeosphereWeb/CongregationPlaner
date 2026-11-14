package de.geosphere.congregationplaner.event.testing

import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.event.domain.model.SpecialEvent
import de.geosphere.congregationplaner.event.domain.repository.SpecialEventRepository
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** In-Memory-[SpecialEventRepository]; neue Ereignisse bekommen die IDs `ev-1`, `ev-2` … */
internal class FakeSpecialEventRepository(events: List<SpecialEvent> = emptyList()) : SpecialEventRepository {
    val events = MutableStateFlow(events)

    var saveResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)

    /** Lässt [observeAll] mit diesem Fehler enden. */
    var observeError: Throwable? = null

    val savedEvents = mutableListOf<SpecialEvent>()
    val deletedIds = mutableListOf<String>()

    override fun observeAll(congregation: CongregationRef): Flow<List<SpecialEvent>> =
        observeError?.let { error -> flow { throw error } } ?: events

    /** Die Gruppen der letzten Abfrage über [observeFor]. */
    var requestedGroups: Set<EventAudience>? = null

    override fun observeFor(congregation: CongregationRef, groups: Set<EventAudience>): Flow<List<SpecialEvent>> {
        requestedGroups = groups
        return observeAll(congregation).map { events -> events.filter { it.audience.any(groups::contains) } }
    }

    override suspend fun save(congregation: CongregationRef, event: SpecialEvent): Result<SpecialEvent> =
        saveResult.map {
            val saved = if (event.id.isBlank()) event.copy(id = "ev-${savedEvents.size + 1}") else event
            savedEvents += saved
            events.value = events.value.filterNot { it.id == saved.id } + saved
            saved
        }

    override suspend fun delete(congregation: CongregationRef, id: String): Result<Unit> = deleteResult.onSuccess {
        deletedIds += id
        events.value = events.value.filterNot { it.id == id }
    }
}
