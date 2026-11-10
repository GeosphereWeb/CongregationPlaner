package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectoryException
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

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
            mobilePhone = speaker.mobilePhone.trim(),
            landlinePhone = speaker.landlinePhone.trim(),
            email = speaker.email.trim(),
            talkNumbers = speaker.talkNumbers.distinct().sorted(),
        )
        val valid = cleaned.lastName.isNotEmpty() &&
            maxOf(cleaned.lastName.length, cleaned.firstName.length) <= MAX_NAME &&
            maxOf(cleaned.mobilePhone.length, cleaned.landlinePhone.length) <= VisitingSpeaker.MAX_PHONE_LENGTH &&
            cleaned.ranking in 0..VisitingSpeaker.MAX_RANKING &&
            cleaned.email.length <= VisitingSpeaker.MAX_EMAIL_LENGTH &&
            cleaned.talkNumbers.size <= VisitingSpeaker.MAX_TALKS &&
            cleaned.talkNumbers.all { it in 1..Talk.MAX_NUMBER } &&
            repository.observeCongregations(congregation).first().any { it.id == cleaned.congregationId }
        if (!valid) return Result.failure(ExternalDirectoryException.InvalidSpeaker())
        return repository.saveSpeaker(congregation, cleaned)
    }
}
