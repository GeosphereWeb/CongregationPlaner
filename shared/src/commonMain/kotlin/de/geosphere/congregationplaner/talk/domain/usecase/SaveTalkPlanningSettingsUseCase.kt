package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkPlanningSettings
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkPlanningSettingsRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/** Speichert die Stichtage der Versammlung des Nutzers; nur Planer dürfen sie ändern. */
@Inject
class SaveTalkPlanningSettingsUseCase(
    private val repository: TalkPlanningSettingsRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, settings: TalkPlanningSettings): Result<TalkPlanningSettings> {
        val congregation = user.congregation
        if (congregation == null || !policy.canPlanTalks(user)) {
            return Result.failure(TalkScheduleException.NotAllowed())
        }
        return repository.save(congregation, settings)
    }
}
