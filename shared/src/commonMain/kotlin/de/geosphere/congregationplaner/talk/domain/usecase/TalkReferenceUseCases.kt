package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkReferenceException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.permission.TalkReferenceDataPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/** Legt einen Vortrag der globalen Liste an oder ändert ihn; Nummer 1 bis [Talk.MAX_NUMBER], Titel zugeschnitten. */
@Inject
class SaveTalkUseCase(private val repository: TalkCatalogRepository, private val policy: TalkReferenceDataPolicy) {
    suspend operator fun invoke(user: AppUser, talk: Talk): Result<Talk> {
        if (!policy.canManage(user)) return Result.failure(TalkReferenceException.NotAllowed())
        val cleaned = talk.copy(title = talk.title.trim())
        val valid = cleaned.number in 1..Talk.MAX_NUMBER &&
            cleaned.title.isNotEmpty() &&
            cleaned.title.length <= TalkSlot.MAX_TALK_TITLE_LENGTH
        if (!valid) return Result.failure(TalkReferenceException.InvalidTalk())
        return repository.save(cleaned)
    }
}

/** Entfernt einen Vortrag aus der Liste; bereits geplante Termine behalten ihre Momentaufnahme. */
@Inject
class DeleteTalkUseCase(private val repository: TalkCatalogRepository, private val policy: TalkReferenceDataPolicy) {
    suspend operator fun invoke(user: AppUser, number: Int): Result<Unit> {
        if (!policy.canManage(user)) return Result.failure(TalkReferenceException.NotAllowed())
        if (number !in 1..Talk.MAX_NUMBER) return Result.failure(TalkReferenceException.InvalidTalk())
        return repository.delete(number)
    }
}

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
