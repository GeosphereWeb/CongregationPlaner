package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.ScheduleWrite
import de.geosphere.congregationplaner.talk.domain.repository.SpeakerRequestRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.datetime.LocalDate

/** Löscht den Termin vom [date]; eine offene Anfrage ([requestId]) des Termins wird im selben Batch mit gelöscht. */
@Inject
class DeleteTalkSlotUseCase(
    private val repository: TalkScheduleRepository,
    private val requests: SpeakerRequestRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, date: LocalDate, requestId: String? = null): Result<Unit> {
        val congregation = user.congregation
        if (congregation == null || !policy.canPlanTalks(user)) {
            return Result.failure(TalkScheduleException.NotAllowed())
        }
        return if (requestId == null) {
            repository.deleteSlot(congregation, date)
        } else {
            val write = ScheduleWrite(deleteSlotDates = listOf(date), deleteRequestIds = listOf(requestId))
            requests.commit(congregation, write)
        }
    }
}
