package de.geosphere.congregationplaner.event.data

import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.event.domain.repository.EventTypeRepository
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

expect fun createEventTypeRepository(): EventTypeRepository

internal const val EVENT_TYPES_SUBCOLLECTION = "eventTypes"

internal val CongregationRef.eventTypesPath: String get() = "$documentPath/$EVENT_TYPES_SUBCOLLECTION"

/** Baut eine Art aus den gelesenen Feldern; fehlende Werte ergeben leeren Namen, kein Ausfall und das Standard-Icon. */
internal fun eventTypeOf(id: String, name: String?, cancelsTalk: Boolean?, icon: String?): EventType = EventType(
    id = id,
    name = name.orEmpty(),
    cancelsTalk = cancelsTalk ?: false,
    icon = icon?.takeIf(String::isNotBlank) ?: EventType.DEFAULT_ICON,
)

fun unavailableEventTypeRepository(platform: String): EventTypeRepository = object : EventTypeRepository {
    private fun <T> unavailable(): Result<T> =
        Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

    override fun observeEventTypes(congregation: CongregationRef): Flow<List<EventType>> = flow {
        throw UnsupportedOperationException("Firestore is not configured for $platform.")
    }

    override suspend fun save(congregation: CongregationRef, type: EventType): Result<EventType> = unavailable()

    override suspend fun delete(congregation: CongregationRef, id: String): Result<Unit> = unavailable()
}
