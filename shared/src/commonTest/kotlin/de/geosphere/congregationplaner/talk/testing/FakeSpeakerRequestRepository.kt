package de.geosphere.congregationplaner.talk.testing

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.SpeakerRequest
import de.geosphere.congregationplaner.talk.domain.repository.ScheduleWrite
import de.geosphere.congregationplaner.talk.domain.repository.SpeakerRequestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-Memory-[SpeakerRequestRepository]; [commit] wirkt auf die Anfragen hier und auf die Termine in [schedule], damit
 * Tests den Zustand nach dem Batch prüfen können. Alle Batches stehen in [writes].
 */
internal class FakeSpeakerRequestRepository(
    requests: List<SpeakerRequest> = emptyList(),
    val schedule: FakeTalkScheduleRepository = FakeTalkScheduleRepository(),
) : SpeakerRequestRepository {
    val requests = MutableStateFlow(requests)
    val writes = mutableListOf<ScheduleWrite>()
    var commitResult: Result<Unit> = Result.success(Unit)

    override fun observeRequests(congregation: CongregationRef): Flow<List<SpeakerRequest>> = requests

    // Termine schreiben folgt den Einstellungen von [schedule] (saveResult, saveGate) und zeichnet dort mit auf, damit
    // bestehende Tests des Editors gleich bleiben.
    override suspend fun commit(congregation: CongregationRef, write: ScheduleWrite): Result<Unit> {
        if (write.saveSlots.isNotEmpty()) schedule.saveGate?.await()
        val slotResult = if (write.saveSlots.isNotEmpty()) schedule.saveResult else Result.success(Unit)
        return commitResult.mapCatching { slotResult.getOrThrow() }.onSuccess {
            writes += write
            schedule.savedSlots += write.saveSlots
            schedule.deletedDates += write.deleteSlotDates
            val savedDates = write.saveSlots.map { it.date }.toSet()
            schedule.slots.value = (
                schedule.slots.value.filterNot { it.date in savedDates || it.date in write.deleteSlotDates } +
                    write.saveSlots
                ).sortedBy { it.date }
            val savedIds = write.saveRequests.map { it.id }.toSet()
            requests.value = requests.value.filterNot { it.id in savedIds || it.id in write.deleteRequestIds } +
                write.saveRequests
        }
    }
}
