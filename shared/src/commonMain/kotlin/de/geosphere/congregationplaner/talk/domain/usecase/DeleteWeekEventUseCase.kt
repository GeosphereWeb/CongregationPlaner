package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkReferenceException
import de.geosphere.congregationplaner.talk.domain.permission.TalkReferenceDataPolicy
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/** Entfernt ein Wochenereignis; bereits geplante Termine behalten den Namen. */
@Inject
class DeleteWeekEventUseCase(
    private val repository: WeekEventRepository,
    private val policy: TalkReferenceDataPolicy,
) {
    suspend operator fun invoke(user: AppUser, id: String): Result<Unit> {
        if (!policy.canManage(user)) return Result.failure(TalkReferenceException.NotAllowed())
        if (id.isBlank()) return Result.failure(TalkReferenceException.InvalidWeekEvent())
        return repository.delete(id)
    }
}
