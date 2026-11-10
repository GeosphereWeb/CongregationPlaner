package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Die Vortragsliste nach Nummer; auch nicht aktive Vorträge sind enthalten, die Auswahl filtert sie. */
@Inject
class ObserveTalkCatalogUseCase(private val repository: TalkCatalogRepository) {
    operator fun invoke(): Flow<List<Talk>> = repository.observeTalks().map { talks -> talks.sortedBy { it.number } }
}
