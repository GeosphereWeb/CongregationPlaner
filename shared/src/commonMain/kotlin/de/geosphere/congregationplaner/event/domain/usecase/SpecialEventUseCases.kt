package de.geosphere.congregationplaner.event.domain.usecase

import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.event.domain.model.SpecialEvent
import de.geosphere.congregationplaner.event.domain.model.SpecialEventException
import de.geosphere.congregationplaner.event.domain.model.eventGroups
import de.geosphere.congregationplaner.event.domain.permission.SpecialEventPolicy
import de.geosphere.congregationplaner.event.domain.repository.SpecialEventRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/** Alle Ereignisse der Versammlung für die Pflege, nach Beginn und Titel sortiert; ohne Berechtigung leer. */
@Inject
class ObserveManagedEventsUseCase(
    private val repository: SpecialEventRepository,
    private val policy: SpecialEventPolicy,
) {
    operator fun invoke(user: AppUser): Flow<List<SpecialEvent>> {
        val congregation = user.congregation
        if (congregation == null || !policy.canManage(user)) return flowOf(emptyList())
        return repository.observeAll(congregation).map { events ->
            events.sortedWith(byStartAndTitle)
        }
    }
}

/**
 * Die kommenden Ereignisse für die Gruppen des Mitglieds (ADR-0033), nach Beginn sortiert: alles, was heute oder
 * später endet. Ohne Freigabe leer, ohne dass etwas abgefragt wird (die Rules würden es ablehnen).
 */
@Inject
class ObserveUpcomingEventsUseCase(
    private val repository: SpecialEventRepository,
    private val policy: SpecialEventPolicy,
) {
    operator fun invoke(user: AppUser, today: LocalDate): Flow<List<SpecialEvent>> {
        val congregation = user.congregation
        if (congregation == null || !policy.canSeeEvents(user)) return flowOf(emptyList())
        return repository.observeFor(congregation, user.profile.eventGroups()).map { events ->
            events.filter { it.endDate >= today }.sortedWith(byStartAndTitle)
        }
    }
}

private val byStartAndTitle = compareBy<SpecialEvent> { it.startDate }.thenBy { it.title.lowercase() }

/**
 * Legt ein Ereignis an oder ändert es: Der Titel wird zugeschnitten und ist Pflicht, das Ende liegt nicht vor dem
 * Beginn, und es braucht mindestens eine Gruppe. Mit „Alle“ zählen die übrigen Gruppen nicht und entfallen.
 */
@Inject
class SaveSpecialEventUseCase(private val repository: SpecialEventRepository, private val policy: SpecialEventPolicy) {
    suspend operator fun invoke(user: AppUser, event: SpecialEvent): Result<SpecialEvent> {
        val congregation = user.congregation
        if (congregation == null || !policy.canManage(user)) return Result.failure(SpecialEventException.NotAllowed())
        val cleaned = event.copy(
            title = event.title.trim(),
            audience = if (EventAudience.ALL in event.audience) setOf(EventAudience.ALL) else event.audience,
        )
        val error = when {
            cleaned.title.isEmpty() || cleaned.title.length > SpecialEvent.MAX_TITLE_LENGTH ->
                SpecialEventException.InvalidTitle()
            cleaned.endDate < cleaned.startDate -> SpecialEventException.InvalidDates()
            cleaned.audience.isEmpty() -> SpecialEventException.MissingAudience()
            else -> null
        }
        return error?.let { Result.failure(it) } ?: repository.save(congregation, cleaned)
    }
}

@Inject
class DeleteSpecialEventUseCase(
    private val repository: SpecialEventRepository,
    private val policy: SpecialEventPolicy,
) {
    suspend operator fun invoke(user: AppUser, id: String): Result<Unit> {
        val congregation = user.congregation
        return when {
            congregation == null || !policy.canManage(user) -> Result.failure(SpecialEventException.NotAllowed())
            id.isBlank() -> Result.failure(IllegalArgumentException("Das Ereignis hat keine ID."))
            else -> repository.delete(congregation, id)
        }
    }
}
