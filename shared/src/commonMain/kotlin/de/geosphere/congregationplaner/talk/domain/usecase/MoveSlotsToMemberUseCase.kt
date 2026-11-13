package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.MemberRef
import de.geosphere.congregationplaner.talk.domain.model.SlotPerson
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.withPersonReplaced
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

/**
 * Stellt alle Termine der Versammlung, die [SlotPerson] nennen, in einem Schreibvorgang auf das Mitglied um (siehe
 * [withPersonReplaced]); liefert die Zahl der geänderten Termine. Nur Planer lesen und schreiben Termine.
 */
@Inject
class MoveSlotsToMemberUseCase(
    private val repository: TalkScheduleRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, from: SlotPerson, to: MemberRef): Result<Int> {
        val congregation = user.plannerCongregation(policy)
            ?: return Result.failure(TalkScheduleException.NotAllowed())
        return runCatching { repository.observeAll(congregation).first() }.fold(
            onSuccess = { slots ->
                val changed = slots.mapNotNull { slot -> slot.withPersonReplaced(from, to).takeIf { it != slot } }
                if (changed.isEmpty()) {
                    Result.success(0)
                } else {
                    repository.saveSlots(congregation, changed).map {
                        changed.size
                    }
                }
            },
            onFailure = { Result.failure(it) },
        )
    }
}
