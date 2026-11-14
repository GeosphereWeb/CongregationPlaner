package de.geosphere.congregationplaner.talk.domain.repository

import de.geosphere.congregationplaner.organization.domain.model.CountryRef
import de.geosphere.congregationplaner.talk.domain.model.Talk
import kotlinx.coroutines.flow.Flow

interface TalkCatalogRepository {
    /** Die Vortragsliste des Landes. */
    fun observeTalks(country: CountryRef): Flow<List<Talk>>

    /** Legt den Vortrag an oder überschreibt ihn; die Nummer ist die Dokument-ID. */
    suspend fun save(country: CountryRef, talk: Talk): Result<Talk>

    suspend fun delete(country: CountryRef, number: Int): Result<Unit>
}
