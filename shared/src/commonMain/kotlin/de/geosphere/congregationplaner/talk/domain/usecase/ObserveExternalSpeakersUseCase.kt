package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Die externen Redner als Auswahl für die Vortragsplanung: Name, Name ihrer Versammlung und ihre Vorträge.
 * Redner, deren Versammlung fehlt, werden übersprungen.
 */
@Inject
class ObserveExternalSpeakersUseCase(private val observeDirectory: ObserveExternalDirectoryUseCase) {
    operator fun invoke(user: AppUser): Flow<List<ExternalSpeaker>> = observeDirectory(user).map { directory ->
        val names = directory.congregations.associate { it.id to it.name }
        directory.speakers.mapNotNull { speaker ->
            names[speaker.congregationId]?.let {
                ExternalSpeaker(
                    speaker.id,
                    speaker.lastName,
                    speaker.firstName,
                    it,
                    speaker.mobilePhone,
                    speaker.landlinePhone,
                    speaker.talkNumbers,
                )
            }
        }
    }
}
