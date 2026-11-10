package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.repository.TalkHistoryRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

/** Die Vortragshistorie der Versammlung des Nutzers, nach Vortragsnummer sortiert. */
@Inject
class ObserveTalkHistoryUseCase(private val repository: TalkHistoryRepository) {
    operator fun invoke(user: AppUser): Flow<List<TalkHistoryEntry>> = user.congregation?.let { congregation ->
        repository.observeAll(congregation).map { entries -> entries.sortedBy { it.talkNumber } }
    } ?: emptyFlow()
}
