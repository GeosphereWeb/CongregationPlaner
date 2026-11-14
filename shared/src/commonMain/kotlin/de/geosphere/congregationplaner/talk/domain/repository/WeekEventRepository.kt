package de.geosphere.congregationplaner.talk.domain.repository

import de.geosphere.congregationplaner.organization.domain.model.CountryRef
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import kotlinx.coroutines.flow.Flow

interface WeekEventRepository {
    /** Die Wochenereignisse des Landes. */
    fun observeWeekEvents(country: CountryRef): Flow<List<WeekEvent>>

    /** Legt das Ereignis an, wenn [WeekEvent.id] leer ist, sonst überschreibt es ihn; liefert es mit seiner ID. */
    suspend fun save(country: CountryRef, event: WeekEvent): Result<WeekEvent>

    suspend fun delete(country: CountryRef, id: String): Result<Unit>
}
