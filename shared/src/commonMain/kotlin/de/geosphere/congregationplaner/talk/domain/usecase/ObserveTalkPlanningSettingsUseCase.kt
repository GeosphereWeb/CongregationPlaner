package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkPlanningSettings
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkPlanningSettingsRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Die Stichtage der Versammlung des Nutzers; nichts ohne Versammlung oder ohne Einblick in die Vortragsplanung. */
@Inject
class ObserveTalkPlanningSettingsUseCase(
    private val repository: TalkPlanningSettingsRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    operator fun invoke(user: AppUser): Flow<TalkPlanningSettings> =
        user.congregation?.takeIf { policy.canViewTalkPlanning(user) }?.let(repository::observe) ?: emptyFlow()
}
