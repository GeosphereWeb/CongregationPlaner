package de.geosphere.congregationplaner.talk.domain.repository

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import kotlinx.coroutines.flow.Flow

/**
 * Das Verzeichnis externer Redner (Kreis → Versammlung → Redner) unter der eigenen Versammlung. Die `save`-Methoden
 * legen den Eintrag an, wenn die ID leer ist, sonst überschreiben sie ihn; sie liefern ihn mit seiner ID.
 */
interface ExternalSpeakerDirectoryRepository {
    fun observeCircuits(congregation: CongregationRef): Flow<List<ExternalCircuit>>

    fun observeCongregations(congregation: CongregationRef): Flow<List<ExternalCongregation>>

    fun observeSpeakers(congregation: CongregationRef): Flow<List<VisitingSpeaker>>

    suspend fun saveCircuit(congregation: CongregationRef, circuit: ExternalCircuit): Result<ExternalCircuit>

    suspend fun saveCongregation(
        congregation: CongregationRef,
        externalCongregation: ExternalCongregation,
    ): Result<ExternalCongregation>

    suspend fun saveSpeaker(congregation: CongregationRef, speaker: VisitingSpeaker): Result<VisitingSpeaker>

    suspend fun deleteCircuit(congregation: CongregationRef, id: String): Result<Unit>

    suspend fun deleteCongregation(congregation: CongregationRef, id: String): Result<Unit>

    suspend fun deleteSpeaker(congregation: CongregationRef, id: String): Result<Unit>
}
