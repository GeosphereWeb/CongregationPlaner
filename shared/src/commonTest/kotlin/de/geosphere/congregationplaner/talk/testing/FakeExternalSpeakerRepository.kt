package de.geosphere.congregationplaner.talk.testing

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-Memory-[ExternalSpeakerRepository]; neue Redner bekommen die IDs `ext-1`, `ext-2` … */
internal class FakeExternalSpeakerRepository(speakers: List<ExternalSpeaker> = emptyList()) :
    ExternalSpeakerRepository {
    val speakers = MutableStateFlow(speakers)

    var saveResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)

    var requestedCongregation: CongregationRef? = null
        private set
    val savedSpeakers = mutableListOf<ExternalSpeaker>()
    val deletedIds = mutableListOf<String>()

    override fun observeAll(congregation: CongregationRef): Flow<List<ExternalSpeaker>> {
        requestedCongregation = congregation
        return speakers
    }

    override suspend fun save(congregation: CongregationRef, speaker: ExternalSpeaker): Result<ExternalSpeaker> =
        saveResult.map {
            val saved = if (speaker.id.isBlank()) speaker.copy(id = "ext-${savedSpeakers.size + 1}") else speaker
            savedSpeakers += saved
            speakers.value = speakers.value.filterNot { it.id == saved.id } + saved
            saved
        }

    override suspend fun delete(congregation: CongregationRef, id: String): Result<Unit> = deleteResult.onSuccess {
        deletedIds += id
        speakers.value = speakers.value.filterNot { it.id == id }
    }
}
