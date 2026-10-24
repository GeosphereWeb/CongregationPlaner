package de.geosphere.congregationplaner.talk.domain.repository

import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import kotlinx.coroutines.flow.Flow

interface WeekEventRepository {
    /** Die globale Liste der Wochenereignisse. */
    fun observeWeekEvents(): Flow<List<WeekEvent>>

    /** Legt das Ereignis an, wenn [WeekEvent.id] leer ist, sonst überschreibt es ihn; liefert es mit seiner ID. */
    suspend fun save(event: WeekEvent): Result<WeekEvent>

    suspend fun delete(id: String): Result<Unit>
}
