package de.geosphere.congregationplaner.talk.presentation.requests

import de.geosphere.congregationplaner.talk.domain.model.OpenRequest
import de.geosphere.congregationplaner.talk.domain.model.SpeakerRequest
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.presentation.common.toDisplayText
import de.geosphere.congregationplaner.ui.list.CardHighlight
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

/** Tag der Anfrage, z. B. „So, 06.12.2026“, „Tag offen“ oder mit „· Termin verstrichen“. */
internal fun requestDayText(open: OpenRequest): String {
    val slot = open.slot ?: return "Tag offen"
    val day = slot.date.toDisplayText()
    return if (open.datePassed) "$day · Termin verstrichen" else day
}

/** Seit wann die Anfrage läuft und der letzte Kontakt, z. B. „seit 10.11. · Anrufbeantworter 12.11.“. */
internal fun requestStatusText(request: SpeakerRequest): String {
    val last = request.contactAttempts.maxByOrNull { it.date }
    val contact = last?.let { "${it.kind.label} ${it.date.shortText()}" } ?: "noch kein Kontakt"
    return "seit ${request.createdOn.shortText()} · $contact"
}

/** Der Entwurf aus dem Dialog „Anfrage ohne Tag“ als Anfrage; die ID vergibt das Speichern. */
internal fun requestFromDraft(draft: TalkSlot, note: String, today: LocalDate): SpeakerRequest = SpeakerRequest(
    id = "",
    createdOn = today,
    note = note.ifBlank { null },
    speakerMemberId = draft.speakerMemberId,
    speakerLastName = draft.speakerLastName,
    speakerFirstName = draft.speakerFirstName,
    externalSpeakerId = draft.externalSpeakerId,
    speakerCongregation = draft.speakerCongregation,
    talkNumber = draft.talkNumber,
    talkTitle = draft.talkTitle,
)

/** Hervorhebung einer Karte: Nachhaken ist fällig oder der Tag ist schon vorbei. */
internal fun requestHighlight(open: OpenRequest): CardHighlight =
    if (open.needsFollowUp || open.datePassed) CardHighlight.Warning else CardHighlight.None

/** Rückfrage vor der Absage; mit Tag wird dieser wieder frei. */
internal fun declineQuestion(open: OpenRequest): String {
    val name = open.speakerDisplayName ?: "dem Redner"
    val day = open.slot?.let { " Der Tag ${it.date.toDisplayText()} wird wieder frei." }.orEmpty()
    return "Die Anfrage bei $name wird mit allen Kontaktversuchen gelöscht.$day"
}

/** Die Notiz eines Kontaktversuchs, gekürzt auf [SpeakerRequest.MAX_ATTEMPT_NOTE_LENGTH] Zeichen. */
internal fun limitAttemptNote(text: String): String = text.take(SpeakerRequest.MAX_ATTEMPT_NOTE_LENGTH)

private fun LocalDate.shortText(): String =
    "${day.toString().padStart(2, '0')}.${month.number.toString().padStart(2, '0')}."
