package de.geosphere.congregationplaner.talk.domain.repository

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkPlanningSettings
import kotlinx.coroutines.flow.Flow

interface TalkPlanningSettingsRepository {
    /** Die Stichtage der Versammlung; ohne gespeichertes Dokument die leeren Einstellungen. */
    fun observe(congregation: CongregationRef): Flow<TalkPlanningSettings>

    suspend fun save(congregation: CongregationRef, settings: TalkPlanningSettings): Result<TalkPlanningSettings>
}
