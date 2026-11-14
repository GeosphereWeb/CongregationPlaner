package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.CountryRef
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Die Vortragsliste nach Nummer; auch nicht aktive Vorträge sind enthalten, die Auswahl filtert sie. */
@Inject
class ObserveTalkCatalogUseCase(private val repository: TalkCatalogRepository) {
    operator fun invoke(country: CountryRef): Flow<List<Talk>> =
        repository.observeTalks(country).map { talks -> talks.sortedBy { it.number } }

    /** Die Liste des Landes der eigenen Versammlung; ohne Versammlung leer. */
    operator fun invoke(user: AppUser): Flow<List<Talk>> =
        user.congregation?.let { invoke(it.country) } ?: flowOf(emptyList())
}
