package de.geosphere.congregationplaner.talk.testing

import de.geosphere.congregationplaner.organization.domain.model.CountryRef
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

    /** Die Länder aller Aufrufe, in Reihenfolge. */
    val countries = mutableListOf<CountryRef>()

    /** Lässt [observeTalks] mit diesem Fehler enden. */
    var observeError: Throwable? = null

    val savedTalks = mutableListOf<Talk>()
    val deletedNumbers = mutableListOf<Int>()

    override fun observeTalks(country: CountryRef): Flow<List<Talk>> {
        countries += country
        return observeError?.let { error -> flow { throw error } } ?: talks
    }

    override suspend fun save(country: CountryRef, talk: Talk): Result<Talk> = saveResult.map {
        countries += country
        savedTalks += talk
        if (publishesChanges) talks.value = talks.value.filterNot { it.number == talk.number } + talk
        talk
    }

    override suspend fun delete(country: CountryRef, number: Int): Result<Unit> = deleteResult.onSuccess {
        deletedNumbers += number
        if (publishesChanges) talks.value = talks.value.filterNot { it.number == number }
    }
}
