package de.geosphere.congregationplaner.talk.testing

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkPlanningSettings
import de.geosphere.congregationplaner.talk.domain.repository.TalkPlanningSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeTalkPlanningSettingsRepository(settings: TalkPlanningSettings = TalkPlanningSettings()) :
    TalkPlanningSettingsRepository {
    val settings = MutableStateFlow(settings)
    var saveResult: Result<Unit> = Result.success(Unit)
    val saved = mutableListOf<TalkPlanningSettings>()
    var requestedCongregation: CongregationRef? = null
        private set

    override fun observe(congregation: CongregationRef): Flow<TalkPlanningSettings> {
        requestedCongregation = congregation
        return settings
    }

    override suspend fun save(
        congregation: CongregationRef,
        settings: TalkPlanningSettings,
    ): Result<TalkPlanningSettings> {
        requestedCongregation = congregation
        return saveResult.map {
            saved += settings
            this.settings.value = settings
            settings
        }
    }
}
