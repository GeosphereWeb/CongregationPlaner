package de.geosphere.congregationplaner.event.domain.repository

import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import kotlinx.coroutines.flow.Flow

/** Zugriff auf die Ereignisarten einer Versammlung (`…/congregation/{id}/eventTypes/{id}`, ADR-0036). */
interface EventTypeRepository {
    fun observeEventTypes(congregation: CongregationRef): Flow<List<EventType>>

    /** Legt die Art an, wenn [EventType.id] leer ist, sonst überschreibt es sie; liefert sie mit ihrer ID. */
    suspend fun save(congregation: CongregationRef, type: EventType): Result<EventType>

    suspend fun delete(congregation: CongregationRef, id: String): Result<Unit>
}
