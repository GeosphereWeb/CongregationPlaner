package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Beobachtet die Termine des Jahres in der Versammlung des Nutzers; ohne Versammlung gibt es keine. */
@Inject
class ObserveTalkYearUseCase(private val repository: TalkScheduleRepository) {
    operator fun invoke(user: AppUser, year: Int): Flow<List<TalkSlot>> =
        user.congregation?.let { repository.observeYear(it, year) } ?: emptyFlow()
}
