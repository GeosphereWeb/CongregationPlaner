package de.geosphere.congregationplaner.event.testing

import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.event.domain.repository.EventTypeRepository
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-Memory-[EventTypeRepository]; neue Arten bekommen die IDs `type-1`, `type-2` … */
internal class FakeEventTypeRepository(types: List<EventType> = emptyList()) : EventTypeRepository {
    val types = MutableStateFlow(types)

    var saveResult: Result<Unit> = Result.success(Unit)

    val savedTypes = mutableListOf<EventType>()
    val deletedIds = mutableListOf<String>()

    override fun observeEventTypes(congregation: CongregationRef): Flow<List<EventType>> = types

    override suspend fun save(congregation: CongregationRef, type: EventType): Result<EventType> = saveResult.map {
        val saved = if (type.id.isBlank()) type.copy(id = "type-${savedTypes.size + 1}") else type
        savedTypes += saved
        types.value = types.value.filterNot { it.id == saved.id } + saved
        saved
    }

    override suspend fun delete(congregation: CongregationRef, id: String): Result<Unit> {
        deletedIds += id
        types.value = types.value.filterNot { it.id == id }
        return Result.success(Unit)
    }
}
