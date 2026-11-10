package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkReferenceException
import de.geosphere.congregationplaner.talk.domain.permission.TalkReferenceDataPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/** Entfernt einen Vortrag aus der Liste; bereits geplante Termine behalten ihre Momentaufnahme. */
@Inject
class DeleteTalkUseCase(private val repository: TalkCatalogRepository, private val policy: TalkReferenceDataPolicy) {
    suspend operator fun invoke(user: AppUser, number: Int): Result<Unit> {
        if (!policy.canManage(user)) return Result.failure(TalkReferenceException.NotAllowed())
        if (number !in 1..Talk.MAX_NUMBER) return Result.failure(TalkReferenceException.InvalidTalk())
        return repository.delete(number)
    }
}
