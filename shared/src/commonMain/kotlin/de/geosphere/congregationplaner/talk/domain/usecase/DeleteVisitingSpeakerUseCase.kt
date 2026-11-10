package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectoryException
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

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
