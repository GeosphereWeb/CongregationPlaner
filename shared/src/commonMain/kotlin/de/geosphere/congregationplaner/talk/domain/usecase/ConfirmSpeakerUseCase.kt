package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.datetime.LocalDate

/**
 * Markiert den Redner des Termins vom [date] als informiert oder nimmt die Markierung zurück. Das dürfen Planer und
 * Vortragsplanungs-Helfer; geändert wird nur das Feld `confirmed`.
 */
@Inject
class ConfirmSpeakerUseCase(
    private val repository: TalkScheduleRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, date: LocalDate, confirmed: Boolean): Result<Unit> {
        val congregation = user.congregation
        if (congregation == null || !policy.canConfirmSpeaker(user)) {
            return Result.failure(TalkScheduleException.NotAllowed())
        }
        return repository.setConfirmed(congregation, date, confirmed)
    }
}
