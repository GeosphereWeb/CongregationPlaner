package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectoryException
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val MAX_NAME = VisitingSpeaker.MAX_NAME_LENGTH

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

/**
 * Die externen Redner als Auswahl für die Vortragsplanung: Name und Name ihrer Versammlung. Redner, deren
 * Versammlung fehlt, werden übersprungen.
 */
@Inject
class ObserveExternalSpeakersUseCase(private val observeDirectory: ObserveExternalDirectoryUseCase) {
    operator fun invoke(user: AppUser): Flow<List<ExternalSpeaker>> = observeDirectory(user).map { directory ->
        val names = directory.congregations.associate { it.id to it.name }
        directory.speakers.mapNotNull { speaker ->
            names[speaker.congregationId]?.let { ExternalSpeaker(speaker.id, speaker.lastName, speaker.firstName, it) }
        }
    }
}

@Inject
class SaveExternalCircuitUseCase(
    private val repository: ExternalSpeakerDirectoryRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, circuit: ExternalCircuit): Result<ExternalCircuit> {
        val congregation = user.plannerCongregation(policy)
            ?: return Result.failure(ExternalDirectoryException.NotAllowed())
        val cleaned = circuit.copy(name = circuit.name.trim())
        if (cleaned.name.isEmpty() || cleaned.name.length > MAX_NAME) {
            return Result.failure(ExternalDirectoryException.InvalidCircuit())
        }
        return repository.saveCircuit(congregation, cleaned)
    }
}

@Inject
class SaveExternalCongregationUseCase(
    private val repository: ExternalSpeakerDirectoryRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(
        user: AppUser,
        externalCongregation: ExternalCongregation,
    ): Result<ExternalCongregation> {
        val congregation = user.plannerCongregation(policy)
            ?: return Result.failure(ExternalDirectoryException.NotAllowed())
        val cleaned = externalCongregation.copy(name = externalCongregation.name.trim())
        val valid = cleaned.name.isNotEmpty() &&
            cleaned.name.length <= MAX_NAME &&
            repository.observeCircuits(congregation).first().any { it.id == cleaned.circuitId }
        if (!valid) return Result.failure(ExternalDirectoryException.InvalidCongregation())
        return repository.saveCongregation(congregation, cleaned)
    }
}

/** Legt einen Redner an oder ändert ihn; Familienname und eine vorhandene Versammlung sind Pflicht. */
@Inject
class SaveVisitingSpeakerUseCase(
    private val repository: ExternalSpeakerDirectoryRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, speaker: VisitingSpeaker): Result<VisitingSpeaker> {
        val congregation = user.plannerCongregation(policy)
            ?: return Result.failure(ExternalDirectoryException.NotAllowed())
        val cleaned = speaker.copy(
            lastName = speaker.lastName.trim(),
            firstName = speaker.firstName.trim(),
            phone = speaker.phone.trim(),
            email = speaker.email.trim(),
            talkNumbers = speaker.talkNumbers.distinct().sorted(),
        )
        val valid = cleaned.lastName.isNotEmpty() &&
            maxOf(cleaned.lastName.length, cleaned.firstName.length) <= MAX_NAME &&
            cleaned.phone.length <= VisitingSpeaker.MAX_PHONE_LENGTH &&
            cleaned.email.length <= VisitingSpeaker.MAX_EMAIL_LENGTH &&
            cleaned.talkNumbers.size <= VisitingSpeaker.MAX_TALKS &&
            cleaned.talkNumbers.all { it in 1..Talk.MAX_NUMBER } &&
            repository.observeCongregations(congregation).first().any { it.id == cleaned.congregationId }
        if (!valid) return Result.failure(ExternalDirectoryException.InvalidSpeaker())
        return repository.saveSpeaker(congregation, cleaned)
    }
}

/** Entfernt einen Kreis; er muss leer sein, damit keine Versammlungen ohne Kreis zurückbleiben. */
@Inject
class DeleteExternalCircuitUseCase(
    private val repository: ExternalSpeakerDirectoryRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, id: String): Result<Unit> {
        val congregation = user.plannerCongregation(policy)
            ?: return Result.failure(ExternalDirectoryException.NotAllowed())
        if (id.isBlank()) return Result.failure(ExternalDirectoryException.InvalidCircuit())
        if (repository.observeCongregations(congregation).first().any { it.circuitId == id }) {
            return Result.failure(ExternalDirectoryException.CircuitNotEmpty())
        }
        return repository.deleteCircuit(congregation, id)
    }
}

/** Entfernt eine Versammlung; sie muss leer sein. */
@Inject
class DeleteExternalCongregationUseCase(
    private val repository: ExternalSpeakerDirectoryRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, id: String): Result<Unit> {
        val congregation = user.plannerCongregation(policy)
            ?: return Result.failure(ExternalDirectoryException.NotAllowed())
        if (id.isBlank()) return Result.failure(ExternalDirectoryException.InvalidCongregation())
        if (repository.observeSpeakers(congregation).first().any { it.congregationId == id }) {
            return Result.failure(ExternalDirectoryException.CongregationNotEmpty())
        }
        return repository.deleteCongregation(congregation, id)
    }
}

/** Entfernt einen Redner; bereits geplante Termine behalten seinen Namen als Momentaufnahme. */
@Inject
class DeleteVisitingSpeakerUseCase(
    private val repository: ExternalSpeakerDirectoryRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, id: String): Result<Unit> {
        val congregation = user.plannerCongregation(policy)
            ?: return Result.failure(ExternalDirectoryException.NotAllowed())
        if (id.isBlank()) return Result.failure(ExternalDirectoryException.InvalidSpeaker())
        return repository.deleteSpeaker(congregation, id)
    }
}

private fun AppUser.plannerCongregation(policy: TalkSchedulePermissionPolicy): CongregationRef? =
    congregation?.takeIf { policy.canPlanTalks(this) }
