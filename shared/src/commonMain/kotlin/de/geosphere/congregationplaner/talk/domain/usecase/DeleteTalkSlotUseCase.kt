package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.datetime.LocalDate

@Inject
class DeleteTalkSlotUseCase(
    private val repository: TalkScheduleRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, date: LocalDate): Result<Unit> {
        val congregation = user.congregation
        if (congregation == null || !policy.canPlanTalks(user)) {
            return Result.failure(TalkScheduleException.NotAllowed())
        }
        return repository.deleteSlot(congregation, date)
    }
}
