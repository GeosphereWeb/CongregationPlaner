package de.geosphere.congregationplaner.talk.domain.repository

import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import kotlinx.coroutines.flow.Flow

interface WeekEventRepository {
    /** Die globale Liste der Wochenereignisse; sie wird außerhalb der App (Firebase-Konsole) gepflegt. */
    fun observeWeekEvents(): Flow<List<WeekEvent>>
}
