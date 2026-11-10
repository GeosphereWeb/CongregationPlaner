package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalTime

/**
 * Setzt die Uhrzeit aller Termine des Jahres auf [time]; alle übrigen Felder der Termine bleiben unverändert.
 * Termine, die schon diese Uhrzeit haben, werden nicht geschrieben. Liefert die Zahl der geänderten Termine.
 * Schlägt ein Schreiben fehl, bricht die Änderung ab; bereits geänderte Termine behalten die neue Uhrzeit.
 */
@Inject
class ChangeYearSlotTimesUseCase(
    private val repository: TalkScheduleRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, year: Int, time: LocalTime): Result<Int> {
        val congregation = user.congregation
        if (congregation == null || !policy.canPlanTalks(user)) {
            return Result.failure(TalkScheduleException.NotAllowed())
        }
        val outdated = try {
            repository.observeYear(congregation, year).first().filter { it.time != time }
        } catch (exception: CancellationException) {
            throw exception
        } catch (@Suppress("TooGenericExceptionCaught") exception: Exception) {
            return Result.failure(exception)
        }
        outdated.forEach { slot ->
            repository.saveSlot(congregation, slot.copy(time = time)).onFailure { return Result.failure(it) }
        }
        return Result.success(outdated.size)
    }
}
