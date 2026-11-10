package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow

/** Das Verzeichnis externer Redner der Versammlung des Nutzers, überall nach Namen sortiert. */
@Inject
class ObserveExternalDirectoryUseCase(private val repository: ExternalSpeakerDirectoryRepository) {
    operator fun invoke(user: AppUser): Flow<ExternalDirectory> = user.congregation?.let { congregation ->
        combine(
            repository.observeCircuits(congregation),
            repository.observeCongregations(congregation),
            repository.observeSpeakers(congregation),
        ) { circuits, congregations, speakers ->
            ExternalDirectory(
                circuits = circuits.sortedBy { it.name.lowercase() },
                congregations = congregations.sortedBy { it.name.lowercase() },
                speakers = speakers.sortedWith(compareBy({ it.lastName.lowercase() }, { it.firstName.lowercase() })),
            )
        }
    } ?: emptyFlow()
}
