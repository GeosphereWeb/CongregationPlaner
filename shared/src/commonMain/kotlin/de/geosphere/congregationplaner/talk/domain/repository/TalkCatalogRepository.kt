package de.geosphere.congregationplaner.talk.domain.repository

import de.geosphere.congregationplaner.talk.domain.model.Talk
import kotlinx.coroutines.flow.Flow

interface TalkCatalogRepository {
    /** Die globale Vortragsliste. */
    fun observeTalks(): Flow<List<Talk>>

    /** Legt den Vortrag an oder überschreibt ihn; die Nummer ist die Dokument-ID. */
    suspend fun save(talk: Talk): Result<Talk>

    suspend fun delete(number: Int): Result<Unit>
}
