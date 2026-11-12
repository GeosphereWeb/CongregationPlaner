package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Alle Termine der Versammlung des Nutzers über alle Jahre; Grundlage der Vorschläge des Planungsassistenten. */
@Inject
class ObserveAllTalkSlotsUseCase(private val repository: TalkScheduleRepository) {
    operator fun invoke(user: AppUser): Flow<List<TalkSlot>> =
        user.congregation?.let(repository::observeAll) ?: emptyFlow()
}
