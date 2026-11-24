package de.geosphere.congregationplaner.talk.domain.repository

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.SpeakerRequest
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Änderungen an Terminen und Anfragen, die zusammen gelingen oder zusammen scheitern. */
data class ScheduleWrite(
    val saveSlots: List<TalkSlot> = emptyList(),
    val deleteSlotDates: List<LocalDate> = emptyList(),
    val saveRequests: List<SpeakerRequest> = emptyList(),
    val deleteRequestIds: List<String> = emptyList(),
)

/** Die offenen Redneranfragen der Versammlung in `talkRequests` (ADR-0039). */
interface SpeakerRequestRepository {
    fun observeRequests(congregation: CongregationRef): Flow<List<SpeakerRequest>>

    /** Schreibt alles in [write] in einem Batch: entweder alles oder nichts. */
    suspend fun commit(congregation: CongregationRef, write: ScheduleWrite): Result<Unit>
}
