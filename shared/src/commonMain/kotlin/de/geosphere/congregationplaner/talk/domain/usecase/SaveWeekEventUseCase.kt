package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkReferenceException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.permission.TalkReferenceDataPolicy
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/** Legt ein Wochenereignis an oder ändert es; der Name ist Pflicht und wird zugeschnitten. */
@Inject
class SaveWeekEventUseCase(private val repository: WeekEventRepository, private val policy: TalkReferenceDataPolicy) {
    suspend operator fun invoke(user: AppUser, event: WeekEvent): Result<WeekEvent> {
        if (!policy.canManage(user)) return Result.failure(TalkReferenceException.NotAllowed())
        val cleaned = event.copy(name = event.name.trim())
        if (cleaned.name.isEmpty() || cleaned.name.length > TalkSlot.MAX_WEEK_EVENT_NAME_LENGTH) {
            return Result.failure(TalkReferenceException.InvalidWeekEvent())
        }
        return repository.save(cleaned)
    }
}
