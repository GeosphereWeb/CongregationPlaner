package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/**
 * Speichert einen Termin in der Versammlung des Nutzers. Der Schlüssel ist das Datum, ein Termin pro Tag.
 * Der Wochentag wird bewusst nicht geprüft, weil sich der Versammlungstag kurzfristig verschieben kann.
 */
@Inject
class SaveTalkSlotUseCase(
    private val repository: TalkScheduleRepository,
    private val policy: TalkSchedulePermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, slot: TalkSlot): Result<TalkSlot> {
        val congregation = user.congregation
        if (congregation == null || !policy.canPlanTalks(user)) {
            return Result.failure(TalkScheduleException.NotAllowed())
        }
        val cleaned = slot.trimmed()
        cleaned.validationError()?.let { return Result.failure(it) }
        return repository.saveSlot(congregation, cleaned)
    }
}

private fun TalkSlot.trimmed(): TalkSlot = copy(
    weekEventName = weekEventName?.trim(),
    talkTitle = talkTitle?.trim(),
    speakerLastName = speakerLastName?.trim(),
    speakerFirstName = speakerFirstName?.trim(),
    chairmanLastName = chairmanLastName?.trim(),
    chairmanFirstName = chairmanFirstName?.trim(),
    speakerCongregation = speakerCongregation?.trim(),
    note = note?.trim()?.ifEmpty { null },
)

private fun TalkSlot.validationError(): TalkScheduleException? = when {
    !hasValidWeekEvent() -> TalkScheduleException.InvalidWeekEvent()
    !hasValidTalk() -> TalkScheduleException.InvalidTalk()
    !hasValidSpeaker() -> TalkScheduleException.InvalidSpeaker()
    (note?.length ?: 0) > TalkSlot.MAX_NOTE_LENGTH -> TalkScheduleException.InvalidNote()
    !hasValidMember(chairmanMemberId, chairmanLastName, chairmanFirstName) -> TalkScheduleException.InvalidChairman()
    speakerMemberId != null && speakerMemberId == chairmanMemberId -> TalkScheduleException.SpeakerIsChairman()
    else -> null
}

private fun TalkSlot.hasValidWeekEvent(): Boolean = if (weekEventId == null) {
    weekEventName == null
} else {
    weekEventId.isNotBlank() &&
        !weekEventName.isNullOrEmpty() &&
        weekEventName.length <= TalkSlot.MAX_WEEK_EVENT_NAME_LENGTH
}

private fun TalkSlot.hasValidTalk(): Boolean = if (talkNumber == null) {
    talkTitle == null
} else {
    talkNumber >= 1 && !talkTitle.isNullOrEmpty() && talkTitle.length <= TalkSlot.MAX_TALK_TITLE_LENGTH
}

// Ein Mitglied oder ein externer Redner mit Versammlung, nie beides.
private fun TalkSlot.hasValidSpeaker(): Boolean = when {
    speakerMemberId != null && externalSpeakerId != null -> false
    externalSpeakerId != null ->
        hasValidMember(externalSpeakerId, speakerLastName, speakerFirstName) &&
            !speakerCongregation.isNullOrEmpty() &&
            speakerCongregation.length <= TalkSlot.MAX_PERSON_NAME_LENGTH
    else -> speakerCongregation == null && hasValidMember(speakerMemberId, speakerLastName, speakerFirstName)
}

// Mitglied-ID, Familienname und Vorname gehören zusammen (alle gesetzt oder alle leer).
private fun hasValidMember(memberId: String?, lastName: String?, firstName: String?): Boolean = if (memberId == null) {
    lastName == null && firstName == null
} else {
    memberId.isNotBlank() &&
        !lastName.isNullOrEmpty() &&
        firstName != null &&
        maxOf(lastName.length, firstName.length) <= TalkSlot.MAX_PERSON_NAME_LENGTH
}
