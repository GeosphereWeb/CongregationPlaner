package de.geosphere.congregationplaner.home.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.model.EventHorizon
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.event.domain.permission.EventTypePolicy
import de.geosphere.congregationplaner.event.domain.usecase.ObserveEventTypesUseCase
import de.geosphere.congregationplaner.event.domain.usecase.ObserveUpcomingEventsUseCase
import de.geosphere.congregationplaner.home.domain.model.HomeEvent
import de.geosphere.congregationplaner.home.domain.model.homeEvents
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/**
 * Die Termine der Vortragsplanung mit Wochenereignis von [today] bis zum Ende des Zeitraums, den der
 * Versammlungsadmin eingestellt hat (ADR-0037). Für freigegebene Mitglieder; sonst leer, ohne dass abgefragt wird.
 */
@Inject
class ObserveScheduleEventsUseCase(
    private val schedule: TalkScheduleRepository,
    private val congregations: CongregationMetadataRepository,
    private val policy: EventTypePolicy,
) {
    operator fun invoke(user: AppUser, today: LocalDate): Flow<List<TalkSlot>> {
        val congregation = user.congregation
        if (congregation == null || !policy.canView(user)) return flowOf(emptyList())
        val horizon = congregations.observeAll(congregation.circuitIds).map { all ->
            all.firstOrNull { it.id == congregation.congregationId }?.eventHorizon ?: EventHorizon()
        }
        return combine(horizon, schedule.observeWeekEventSlots(congregation)) { range, slots ->
            val lastDay = range.lastDay(today)
            slots.filter { it.date in today..lastDay }
        }
    }
}

/**
 * „Besondere Ereignisse“ der Startseite: die Ereignisse des Admins und die Termine mit Wochenereignis, mit Titel und
 * Icon aus den Ereignisarten. Ohne Ereignisarten oder Termine (z. B. Regeln noch nicht deployt) bleiben die Ereignisse
 * des Admins sichtbar.
 */
@Inject
class ObserveHomeEventsUseCase(
    private val observeEvents: ObserveUpcomingEventsUseCase,
    private val observeScheduleEvents: ObserveScheduleEventsUseCase,
    private val observeEventTypes: ObserveEventTypesUseCase,
) {
    operator fun invoke(user: AppUser, today: LocalDate): Flow<List<HomeEvent>> = combine(
        observeEvents(user, today),
        observeScheduleEvents(user, today).catch { emit(emptyList()) },
        observeEventTypes(user).catch { emit(emptyList()) },
        ::homeEvents,
    )
}
