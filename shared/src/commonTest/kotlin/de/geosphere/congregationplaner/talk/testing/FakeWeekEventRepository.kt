package de.geosphere.congregationplaner.talk.testing

import de.geosphere.congregationplaner.organization.domain.model.CountryRef
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/** In-Memory-[WeekEventRepository]; neue Ereignisse bekommen die IDs `ev-1`, `ev-2` … */
internal class FakeWeekEventRepository(events: List<WeekEvent> = emptyList()) : WeekEventRepository {
    val events = MutableStateFlow(events)

    /** `false` simuliert einen verzögerten Snapshot-Flow: Schreibvorgänge erreichen [observeWeekEvents] nicht. */
    var publishesChanges = true

    var saveResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)

    /** Die Länder aller Aufrufe, in Reihenfolge. */
    val countries = mutableListOf<CountryRef>()

    /** Lässt [observeWeekEvents] mit diesem Fehler enden. */
    var observeError: Throwable? = null

    val savedEvents = mutableListOf<WeekEvent>()
    val deletedIds = mutableListOf<String>()

    override fun observeWeekEvents(country: CountryRef): Flow<List<WeekEvent>> {
        countries += country
        return observeError?.let { error -> flow { throw error } } ?: events
    }

    override suspend fun save(country: CountryRef, event: WeekEvent): Result<WeekEvent> = saveResult.map {
        countries += country
        val saved = if (event.id.isBlank()) event.copy(id = "ev-${savedEvents.size + 1}") else event
        savedEvents += saved
        if (publishesChanges) events.value = events.value.filterNot { it.id == saved.id } + saved
        saved
    }

    override suspend fun delete(country: CountryRef, id: String): Result<Unit> = deleteResult.onSuccess {
        deletedIds += id
        if (publishesChanges) events.value = events.value.filterNot { it.id == id }
    }
}
