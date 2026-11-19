package de.geosphere.congregationplaner.event.domain.usecase

import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.event.domain.model.EventTypeException
import de.geosphere.congregationplaner.event.domain.permission.EventTypePolicy
import de.geosphere.congregationplaner.event.domain.repository.EventTypeRepository
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Die Ereignisarten der eigenen Versammlung, nach Name sortiert; ohne Freigabe leer, ohne dass abgefragt wird. */
@Inject
class ObserveEventTypesUseCase(private val repository: EventTypeRepository, private val policy: EventTypePolicy) {
    operator fun invoke(user: AppUser): Flow<List<EventType>> {
        val congregation = user.congregation
        if (congregation == null || !policy.canView(user)) return flowOf(emptyList())
        return repository.observeEventTypes(congregation).map { types -> types.sortedBy { it.name.lowercase() } }
    }
}

/**
 * Legt eine Ereignisart an oder ändert sie: Der Name wird zugeschnitten und ist Pflicht, das Icon muss ein Icon des
 * App-Satzes sein. Ändern darf nur der Versammlungsadmin, anlegen auch der Vortragsplaner (Import).
 */
@Inject
class SaveEventTypeUseCase(private val repository: EventTypeRepository, private val policy: EventTypePolicy) {
    suspend operator fun invoke(user: AppUser, type: EventType): Result<EventType> {
        val congregation = user.congregation
        val allowed = if (type.id.isBlank()) policy.canCreate(user) else policy.canManage(user)
        if (congregation == null || !allowed) return Result.failure(EventTypeException.NotAllowed())
        val cleaned = type.copy(name = type.name.trim())
        val error = when {
            cleaned.name.isEmpty() || cleaned.name.length > EventType.MAX_NAME_LENGTH ->
                EventTypeException.InvalidName()
            !EventType.ICON_PATTERN.matches(cleaned.icon) -> EventTypeException.InvalidIcon()
            else -> null
        }
        return error?.let { Result.failure(it) } ?: repository.save(congregation, cleaned)
    }
}

/** Löscht eine Ereignisart; Termine und Ereignisse, die darauf verweisen, behalten ihren Namen (ADR-0036). */
@Inject
class DeleteEventTypeUseCase(private val repository: EventTypeRepository, private val policy: EventTypePolicy) {
    suspend operator fun invoke(user: AppUser, id: String): Result<Unit> {
        val congregation = user.congregation
        return when {
            congregation == null || !policy.canManage(user) -> Result.failure(EventTypeException.NotAllowed())
            id.isBlank() -> Result.failure(IllegalArgumentException("Die Ereignisart hat keine ID."))
            else -> repository.delete(congregation, id)
        }
    }
}

/**
 * Übernimmt die Wochenereignisse des Landes als Vorlage: Ergänzt werden nur Arten, deren Name in der Versammlung
 * fehlt; vorhandene bleiben unverändert. Liefert die Zahl der ergänzten Arten.
 */
@Inject
class AdoptEventTypeBlueprintUseCase(
    private val repository: EventTypeRepository,
    private val blueprint: WeekEventRepository,
    private val policy: EventTypePolicy,
) {
    suspend operator fun invoke(user: AppUser): Result<Int> {
        val congregation = user.congregation
        if (congregation == null || !policy.canManage(user)) return Result.failure(EventTypeException.NotAllowed())
        return runCatching {
            val missing = missingFromBlueprint(
                blueprint.observeWeekEvents(congregation.country).first(),
                repository.observeEventTypes(congregation).first(),
            )
            missing.forEach { repository.save(congregation, it).getOrThrow() }
            missing.size
        }
    }
}

/** Die Vorlagen, deren Name (ohne Groß-/Kleinschreibung und Leerraum am Rand) noch keine Art trägt, als neue Arten. */
internal fun missingFromBlueprint(blueprint: List<WeekEvent>, existing: List<EventType>): List<EventType> {
    val known = existing.map { it.name.trim().lowercase() }.toMutableSet()
    return blueprint.mapNotNull { event ->
        val name = event.name.trim()
        if (name.isEmpty() ||
            !known.add(name.lowercase())
        ) {
            null
        } else {
            EventType(name = name, cancelsTalk = event.cancelsTalk)
        }
    }
}
