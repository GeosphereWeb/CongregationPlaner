package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.datesOfWeekdayIn
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Legt für ein Jahr alle Termine am Wochenende auf einmal an: den Wochentag und die Uhrzeit der Zusammenkunft am
 * Wochenende, die für dieses Jahr in den Versammlungsdaten stehen (Jahreseintrag oder Standard). Ein Tag, für den
 * es schon einen Termin gibt, bleibt unverändert; nichts wird überschrieben.
 */
@Inject
class CreateYearSlotsUseCase(
    private val repository: TalkScheduleRepository,
    private val congregations: CongregationMetadataRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, year: Int): Result<YearSlotsResult> {
        val congregation = user.congregation
        if (congregation == null || !policy.canPlanTalks(user)) {
            return Result.failure(TalkScheduleException.NotAllowed())
        }
        val all = try {
            congregations.observeAll(congregation.circuitIds).first()
        } catch (exception: CancellationException) {
            throw exception
        } catch (@Suppress("TooGenericExceptionCaught") exception: Exception) {
            return Result.failure(exception)
        }
        val times = all.firstOrNull { it.id == congregation.congregationId }?.meetingTimesFor(year)
            ?: return Result.failure(TalkScheduleException.CongregationNotFound())
        val slots = datesOfWeekdayIn(year, times.weekendWeekday).map { TalkSlot(date = it, time = times.weekendTime) }
        return repository.createMissingSlots(congregation, slots)
            .map { created -> YearSlotsResult(created = created, alreadyExisting = slots.size - created) }
    }
}
