package de.geosphere.congregationplaner.event.data

import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.event.domain.model.SpecialEvent
import de.geosphere.congregationplaner.event.domain.repository.SpecialEventRepository
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate

expect fun createSpecialEventRepository(): SpecialEventRepository

internal const val EVENTS_SUBCOLLECTION = "events"
internal const val AUDIENCE_FIELD = "audience"

internal val CongregationRef.eventsPath: String get() = "$documentPath/$EVENTS_SUBCOLLECTION"

/**
 * Baut ein Ereignis aus den gelesenen Feldern; ohne gültigen Beginn wird es übersprungen, ein fehlendes Ende ist der
 * Beginn.
 */
internal fun specialEventOf(
    id: String,
    title: String?,
    startDate: String?,
    endDate: String?,
    audience: List<String?>,
): SpecialEvent? {
    val start = parseEventDate(startDate) ?: return null
    return SpecialEvent(
        id = id,
        title = title.orEmpty(),
        startDate = start,
        endDate = parseEventDate(endDate) ?: start,
        audience = EventAudience.fromValues(audience),
    )
}

private fun parseEventDate(value: String?): LocalDate? = value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/** Die Felder des Dokuments; die Gruppen in fester Reihenfolge, damit gleiche Ereignisse gleich gespeichert werden. */
internal fun SpecialEvent.audienceValues(): List<String> = audience.values()

internal fun Set<EventAudience>.values(): List<String> = sortedBy { it.ordinal }.map { it.name }

fun unavailableSpecialEventRepository(platform: String): SpecialEventRepository = object : SpecialEventRepository {
    private fun <T> unavailable(): Result<T> =
        Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

    override fun observeAll(congregation: CongregationRef): Flow<List<SpecialEvent>> = flow {
        throw UnsupportedOperationException("Firestore is not configured for $platform.")
    }

    override fun observeFor(congregation: CongregationRef, groups: Set<EventAudience>): Flow<List<SpecialEvent>> =
        observeAll(congregation)

    override suspend fun save(congregation: CongregationRef, event: SpecialEvent): Result<SpecialEvent> = unavailable()

    override suspend fun delete(congregation: CongregationRef, id: String): Result<Unit> = unavailable()
}
