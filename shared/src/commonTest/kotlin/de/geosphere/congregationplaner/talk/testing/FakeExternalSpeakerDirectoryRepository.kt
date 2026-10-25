package de.geosphere.congregationplaner.talk.testing

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerDirectoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-Memory-[ExternalSpeakerDirectoryRepository]; neue Einträge bekommen die IDs `c-1`, `g-1` und `s-1` … */
internal class FakeExternalSpeakerDirectoryRepository(
    circuits: List<ExternalCircuit> = emptyList(),
    congregations: List<ExternalCongregation> = emptyList(),
    speakers: List<VisitingSpeaker> = emptyList(),
) : ExternalSpeakerDirectoryRepository {
    val circuits = MutableStateFlow(circuits)
    val congregations = MutableStateFlow(congregations)
    val speakers = MutableStateFlow(speakers)

    var saveResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)

    var requestedCongregation: CongregationRef? = null
        private set
    val savedCircuits = mutableListOf<ExternalCircuit>()
    val savedCongregations = mutableListOf<ExternalCongregation>()
    val savedSpeakers = mutableListOf<VisitingSpeaker>()
    val deletedIds = mutableListOf<String>()

    override fun observeCircuits(congregation: CongregationRef): Flow<List<ExternalCircuit>> {
        requestedCongregation = congregation
        return circuits
    }

    override fun observeCongregations(congregation: CongregationRef): Flow<List<ExternalCongregation>> = congregations

    override fun observeSpeakers(congregation: CongregationRef): Flow<List<VisitingSpeaker>> = speakers

    override suspend fun saveCircuit(congregation: CongregationRef, circuit: ExternalCircuit) = saveResult.map {
        val saved = if (circuit.id.isBlank()) circuit.copy(id = "c-${savedCircuits.size + 1}") else circuit
        savedCircuits += saved
        circuits.value = circuits.value.filterNot { it.id == saved.id } + saved
        saved
    }

    override suspend fun saveCongregation(congregation: CongregationRef, externalCongregation: ExternalCongregation) =
        saveResult.map {
            val saved = externalCongregation.takeUnless { it.id.isBlank() }
                ?: externalCongregation.copy(id = "g-${savedCongregations.size + 1}")
            savedCongregations += saved
            congregations.value = congregations.value.filterNot { it.id == saved.id } + saved
            saved
        }

    override suspend fun saveSpeaker(congregation: CongregationRef, speaker: VisitingSpeaker) = saveResult.map {
        val saved = if (speaker.id.isBlank()) speaker.copy(id = "s-${savedSpeakers.size + 1}") else speaker
        savedSpeakers += saved
        speakers.value = speakers.value.filterNot { it.id == saved.id } + saved
        saved
    }

    override suspend fun deleteCircuit(congregation: CongregationRef, id: String) = deleteResult.onSuccess {
        deletedIds += id
        circuits.value = circuits.value.filterNot { it.id == id }
    }

    override suspend fun deleteCongregation(congregation: CongregationRef, id: String) = deleteResult.onSuccess {
        deletedIds += id
        congregations.value = congregations.value.filterNot { it.id == id }
    }

    override suspend fun deleteSpeaker(congregation: CongregationRef, id: String) = deleteResult.onSuccess {
        deletedIds += id
        speakers.value = speakers.value.filterNot { it.id == id }
    }
}
