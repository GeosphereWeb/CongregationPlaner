package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.model.talkHistoryOf
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

/** Die Vortragshistorie der Versammlung des Nutzers, aus allen Terminen berechnet und nach Vortragsnummer sortiert. */
@Inject
class ObserveTalkHistoryUseCase(private val repository: TalkScheduleRepository) {
    operator fun invoke(user: AppUser): Flow<List<TalkHistoryEntry>> =
        user.congregation?.let { repository.observeAll(it).map(::talkHistoryOf) } ?: emptyFlow()
}
