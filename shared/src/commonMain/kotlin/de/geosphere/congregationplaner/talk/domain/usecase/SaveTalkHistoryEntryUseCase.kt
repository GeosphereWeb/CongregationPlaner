package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkHistoryRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/** Speichert, an welchen Tagen ein Vortrag gehalten wurde; die Tage werden sortiert und von Duplikaten befreit. */
@Inject
class SaveTalkHistoryEntryUseCase(
    private val repository: TalkHistoryRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, entry: TalkHistoryEntry): Result<TalkHistoryEntry> {
        val congregation = user.congregation
        if (congregation == null || !policy.canPlanTalks(user)) {
            return Result.failure(TalkScheduleException.NotAllowed())
        }
        val planned = entry.plannedOn.distinct().sorted()
        val heldOn = (entry.heldOn + planned).distinct().sorted()
        val cleaned = entry.copy(
            heldOn = heldOn,
            plannedOn = planned,
            speakers = entry.speakers.filterKeys { it in heldOn },
        )
        val valid = cleaned.talkNumber in 1..Talk.MAX_NUMBER &&
            cleaned.heldOn.size in 1..TalkHistoryEntry.MAX_DATES
        if (!valid) return Result.failure(TalkScheduleException.InvalidTalkHistory())
        return repository.save(congregation, cleaned)
    }
}
