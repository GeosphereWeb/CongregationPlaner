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
        return slot.checked().fold({ repository.saveSlot(congregation, it) }, { Result.failure(it) })
    }
}

/** Der Termin ohne Leerzeichen am Rand, sofern er gültig ist; sonst der fachliche Fehler. */
internal fun TalkSlot.checked(): Result<TalkSlot> {
    val cleaned = trimmed()
    return cleaned.validationError()?.let { Result.failure(it) } ?: Result.success(cleaned)
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

// Ohne Nummer ist der Titel ein frei eingetragener Vortrag; er darf auch fehlen.
private fun TalkSlot.hasValidTalk(): Boolean = when {
    talkNumber == null -> talkTitle == null || talkTitle.isValidTalkTitle()
    else -> talkNumber >= 1 && talkTitle != null && talkTitle.isValidTalkTitle()
}

private fun String.isValidTalkTitle(): Boolean = isNotEmpty() && length <= TalkSlot.MAX_TALK_TITLE_LENGTH

// Ein Mitglied, ein externer Redner mit Versammlung oder ein frei eingetragener Name, nie mehreres.
private fun TalkSlot.hasValidSpeaker(): Boolean = when {
    speakerMemberId != null && externalSpeakerId != null -> false
    externalSpeakerId != null ->
        hasValidMember(externalSpeakerId, speakerLastName, speakerFirstName) &&
            !speakerCongregation.isNullOrEmpty() &&
            speakerCongregation.length <= TalkSlot.MAX_PERSON_NAME_LENGTH
    speakerMemberId != null ->
        speakerCongregation == null && hasValidMember(speakerMemberId, speakerLastName, speakerFirstName)
    else -> speakerCongregation == null && hasValidFreeName(speakerLastName, speakerFirstName)
}

// Ein frei eingetragener Redner: Name gesetzt (Vorname darf leer sein) oder beides leer.
private fun hasValidFreeName(lastName: String?, firstName: String?): Boolean = if (lastName == null) {
    firstName == null
} else {
    lastName.isNotEmpty() &&
        firstName != null &&
        maxOf(lastName.length, firstName.length) <= TalkSlot.MAX_PERSON_NAME_LENGTH
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
