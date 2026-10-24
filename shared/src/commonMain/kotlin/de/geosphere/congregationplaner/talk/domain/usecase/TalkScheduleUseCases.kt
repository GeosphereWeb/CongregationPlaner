package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/** Beobachtet die Termine des Jahres in der Versammlung des Nutzers; ohne Versammlung gibt es keine. */
@Inject
class ObserveTalkYearUseCase(private val repository: TalkScheduleRepository) {
    operator fun invoke(user: AppUser, year: Int): Flow<List<TalkSlot>> =
        user.congregation?.let { repository.observeYear(it, year) } ?: emptyFlow()
}

@Inject
class ObserveWeekEventsUseCase(private val repository: WeekEventRepository) {
    operator fun invoke(): Flow<List<WeekEvent>> =
        repository.observeWeekEvents().map { events -> events.sortedBy { it.name.lowercase() } }
}

/**
 * Speichert einen Termin in der Versammlung des Nutzers. Der Schlüssel ist das Datum, ein Termin pro Tag.
 * Der Wochentag wird bewusst nicht geprüft, weil sich der Versammlungstag kurzfristig verschieben kann.
 */
@Inject
class SaveTalkSlotUseCase(
    private val repository: TalkScheduleRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, slot: TalkSlot): Result<TalkSlot> {
        val congregation = user.congregation
        if (congregation == null || !policy.canPlanTalks(user)) {
            return Result.failure(TalkScheduleException.NotAllowed())
        }
        val eventName = slot.weekEventName?.trim()
        val validEvent = if (slot.weekEventId == null) {
            eventName == null
        } else {
            slot.weekEventId.isNotBlank() &&
                !eventName.isNullOrEmpty() &&
                eventName.length <= TalkSlot.MAX_WEEK_EVENT_NAME_LENGTH
        }
        if (!validEvent) return Result.failure(TalkScheduleException.InvalidWeekEvent())
        return repository.saveSlot(congregation, slot.copy(weekEventName = eventName))
    }
}

/**
 * Verschiebt einen Termin auf ein anderes Datum. Da das Datum der Schlüssel ist, wird der neue Termin zuerst
 * gespeichert und danach der alte gelöscht: Schlägt das Löschen fehl, bleibt ein Duplikat statt eines Verlusts.
 * Der Aufrufer muss sicherstellen, dass am Zieltag kein anderer Termin liegt, denn Speichern überschreibt.
 */
@Inject
class MoveTalkSlotUseCase(private val save: SaveTalkSlotUseCase, private val delete: DeleteTalkSlotUseCase) {
    suspend operator fun invoke(user: AppUser, from: LocalDate, slot: TalkSlot): Result<TalkSlot> =
        save(user, slot).mapCatching { saved ->
            if (from != saved.date) delete(user, from).getOrThrow()
            saved
        }
}

@Inject
class DeleteTalkSlotUseCase(
    private val repository: TalkScheduleRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, date: LocalDate): Result<Unit> {
        val congregation = user.congregation
        if (congregation == null || !policy.canPlanTalks(user)) {
            return Result.failure(TalkScheduleException.NotAllowed())
        }
        return repository.deleteSlot(congregation, date)
    }
}
