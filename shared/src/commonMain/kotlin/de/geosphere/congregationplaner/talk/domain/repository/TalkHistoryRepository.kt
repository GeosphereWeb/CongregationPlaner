package de.geosphere.congregationplaner.talk.domain.repository

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import kotlinx.coroutines.flow.Flow

interface TalkHistoryRepository {
    fun observeAll(congregation: CongregationRef): Flow<List<TalkHistoryEntry>>

    /** Legt den Eintrag an oder überschreibt ihn; die Vortragsnummer ist die Dokument-ID. */
    suspend fun save(congregation: CongregationRef, entry: TalkHistoryEntry): Result<TalkHistoryEntry>

    suspend fun delete(congregation: CongregationRef, talkNumber: Int): Result<Unit>
}
