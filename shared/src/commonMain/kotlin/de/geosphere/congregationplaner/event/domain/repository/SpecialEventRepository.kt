package de.geosphere.congregationplaner.event.domain.repository

import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.event.domain.model.SpecialEvent
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import kotlinx.coroutines.flow.Flow

/** Zugriff auf die besonderen Ereignisse einer Versammlung (`…/congregation/{id}/events/{id}`). */
interface SpecialEventRepository {
    /** Alle Ereignisse; lesen darf das nur der Versammlungsadmin. */
    fun observeAll(congregation: CongregationRef): Flow<List<SpecialEvent>>

    /**
     * Die Ereignisse, die sich an eine der [groups] richten. Mitglieder dürfen nur so abfragen, und nur mit ihren
     * eigenen Gruppen (ADR-0033).
     */
    fun observeFor(congregation: CongregationRef, groups: Set<EventAudience>): Flow<List<SpecialEvent>>

    /** Legt das Ereignis an, wenn [SpecialEvent.id] leer ist, sonst überschreibt es es; liefert es mit seiner ID. */
    suspend fun save(congregation: CongregationRef, event: SpecialEvent): Result<SpecialEvent>

    suspend fun delete(congregation: CongregationRef, id: String): Result<Unit>
}
