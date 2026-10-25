package de.geosphere.congregationplaner.talk.testing

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/** In-Memory-[TalkCatalogRepository]; Fehler lassen sich über die `*Result`- und `observeError`-Felder einstellen. */
internal class FakeTalkCatalogRepository(talks: List<Talk> = emptyList()) : TalkCatalogRepository {
    val talks = MutableStateFlow(talks)

    /** `false` simuliert einen verzögerten Snapshot-Flow: Schreibvorgänge erreichen [observeTalks] nicht. */
    var publishesChanges = true

    var saveResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)

    /** Lässt [observeTalks] mit diesem Fehler enden. */
    var observeError: Throwable? = null

    val savedTalks = mutableListOf<Talk>()
    val deletedNumbers = mutableListOf<Int>()

    override fun observeTalks(): Flow<List<Talk>> = observeError?.let { error -> flow { throw error } } ?: talks

    override suspend fun save(talk: Talk): Result<Talk> = saveResult.map {
        savedTalks += talk
        if (publishesChanges) talks.value = talks.value.filterNot { it.number == talk.number } + talk
        talk
    }

    override suspend fun delete(number: Int): Result<Unit> = deleteResult.onSuccess {
        deletedNumbers += number
        if (publishesChanges) talks.value = talks.value.filterNot { it.number == number }
    }
}
