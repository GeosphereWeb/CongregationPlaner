package de.geosphere.congregationplaner.talk.testing

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.repository.TalkHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-Memory-[TalkHistoryRepository]; Fehler lassen sich über die `*Result`-Felder einstellen. */
internal class FakeTalkHistoryRepository(entries: List<TalkHistoryEntry> = emptyList()) : TalkHistoryRepository {
    val entries = MutableStateFlow(entries)

    /** `false` simuliert einen verzögerten Snapshot-Flow: Schreibvorgänge erreichen [observeAll] nicht. */
    var publishesChanges = true

    var saveResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)

    var requestedCongregation: CongregationRef? = null
        private set
    val savedEntries = mutableListOf<TalkHistoryEntry>()
    val deletedNumbers = mutableListOf<Int>()

    override fun observeAll(congregation: CongregationRef): Flow<List<TalkHistoryEntry>> {
        requestedCongregation = congregation
        return entries
    }

    override suspend fun save(congregation: CongregationRef, entry: TalkHistoryEntry): Result<TalkHistoryEntry> {
        requestedCongregation = congregation
        return saveResult.map {
            savedEntries += entry
            if (publishesChanges) entries.value = entries.value.filterNot { it.talkNumber == entry.talkNumber } + entry
            entry
        }
    }

    override suspend fun delete(congregation: CongregationRef, talkNumber: Int): Result<Unit> {
        requestedCongregation = congregation
        return deleteResult.onSuccess {
            deletedNumbers += talkNumber
            if (publishesChanges) entries.value = entries.value.filterNot { it.talkNumber == talkNumber }
        }
    }
}
