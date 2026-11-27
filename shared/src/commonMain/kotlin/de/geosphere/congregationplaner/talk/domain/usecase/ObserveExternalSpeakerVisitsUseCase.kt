package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.SpeakerVisits
import de.geosphere.congregationplaner.talk.domain.model.speakerVisitsOf
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take

/**
 * Die Besuche je externem Redner in der Versammlung des Nutzers, aus allen Terminen berechnet (CP-193). Gelesen wird
 * nur einmal: Auf dem Desktop würde [TalkScheduleRepository.observeAll] sonst jede Minute die ganze Planung lesen
 * (Firestore-Kontingent, CP-189), und Besuche ändern sich kaum, solange die Rednerliste offen ist.
 */
@Inject
class ObserveExternalSpeakerVisitsUseCase(private val repository: TalkScheduleRepository) {
    operator fun invoke(user: AppUser): Flow<Map<String, SpeakerVisits>> =
        user.congregation?.let { repository.observeAll(it).take(1).map(::speakerVisitsOf) } ?: emptyFlow()
}
