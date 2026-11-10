package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkHistoryRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/** Entfernt die Historie eines Vortrags; er gilt danach wieder als noch nie gehalten. */
@Inject
class DeleteTalkHistoryEntryUseCase(
    private val repository: TalkHistoryRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, talkNumber: Int): Result<Unit> {
        val congregation = user.congregation
        if (congregation == null || !policy.canPlanTalks(user)) {
            return Result.failure(TalkScheduleException.NotAllowed())
        }
        if (talkNumber !in 1..Talk.MAX_NUMBER) return Result.failure(TalkScheduleException.InvalidTalkHistory())
        return repository.delete(congregation, talkNumber)
    }
}
