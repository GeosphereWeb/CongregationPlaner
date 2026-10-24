package de.geosphere.congregationplaner.talk.domain.repository

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import kotlinx.coroutines.flow.Flow

interface ExternalSpeakerRepository {
    fun observeAll(congregation: CongregationRef): Flow<List<ExternalSpeaker>>

    /** Legt den Redner an, wenn [ExternalSpeaker.id] leer ist, sonst überschreibt es ihn; liefert ihn mit seiner ID. */
    suspend fun save(congregation: CongregationRef, speaker: ExternalSpeaker): Result<ExternalSpeaker>

    suspend fun delete(congregation: CongregationRef, id: String): Result<Unit>
}
