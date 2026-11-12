package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.hasAssignment
import de.geosphere.congregationplaner.talk.domain.model.reassignTalk
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/**
 * Plant Vortrag und Redner von [from] auf den Termin [to] um; ist [to] belegt, tauschen beide Termine ihre Belegung
 * (siehe [reassignTalk]). Beide Termine werden geprüft und zusammen gespeichert, damit nie nur einer geändert ist.
 * Liefert die neuen Termine ([from], [to]).
 */
@Inject
class ReassignTalkUseCase(
    private val repository: TalkScheduleRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, from: TalkSlot, to: TalkSlot): Result<Pair<TalkSlot, TalkSlot>> {
        val congregation = user.congregation
        val error = when {
            congregation == null || !policy.canPlanTalks(user) -> TalkScheduleException.NotAllowed()
            from.date == to.date -> TalkScheduleException.SameDay()
            !from.hasAssignment -> TalkScheduleException.NothingToReassign()
            to.talkCancelled -> TalkScheduleException.TargetCancelled()
            else -> null
        }
        if (error != null || congregation == null) return Result.failure(error ?: TalkScheduleException.NotAllowed())
        val (newFrom, newTo) = reassignTalk(from, to)
        return newFrom.checked().mapCatching { checkedFrom ->
            val checkedTo = newTo.checked().getOrThrow()
            repository.saveSlots(congregation, listOf(checkedFrom, checkedTo)).getOrThrow()
            checkedFrom to checkedTo
        }
    }
}
