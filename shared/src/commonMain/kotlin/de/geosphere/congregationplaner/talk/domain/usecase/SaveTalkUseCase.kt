package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkReferenceException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.permission.TalkReferenceDataPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/**
 * Legt einen Vortrag der globalen Liste an oder ändert ihn; Nummer 1 bis [Talk.MAX_NUMBER], Titel zugeschnitten. Die
 * Titeländerungen werden nach Datum sortiert; je Tag gibt es höchstens eine, insgesamt höchstens
 * [Talk.MAX_TITLE_CHANGES].
 */
@Inject
class SaveTalkUseCase(private val repository: TalkCatalogRepository, private val policy: TalkReferenceDataPolicy) {
    suspend operator fun invoke(user: AppUser, talk: Talk): Result<Talk> {
        if (!policy.canManage(user)) return Result.failure(TalkReferenceException.NotAllowed())
        val cleaned = talk.copy(
            title = talk.title.trim(),
            titleChanges = talk.titleChanges.map { it.copy(title = it.title.trim()) }.sortedBy { it.validFrom },
        )
        val valid = cleaned.number in 1..Talk.MAX_NUMBER &&
            cleaned.title.isValidTitle() &&
            cleaned.titleChanges.size <= Talk.MAX_TITLE_CHANGES &&
            cleaned.titleChanges.all { it.title.isValidTitle() } &&
            cleaned.titleChanges.map { it.validFrom }.distinct().size == cleaned.titleChanges.size
        if (!valid) return Result.failure(TalkReferenceException.InvalidTalk())
        return repository.save(cleaned)
    }
}

private fun String.isValidTitle(): Boolean = isNotEmpty() && length <= TalkSlot.MAX_TALK_TITLE_LENGTH
