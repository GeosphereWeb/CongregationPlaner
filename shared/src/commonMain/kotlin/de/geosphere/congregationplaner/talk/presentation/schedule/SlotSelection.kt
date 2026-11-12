package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.Chairman
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.Speaker
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent

/** Übernimmt das Ereignis samt der Vorbelegung „Vortrag entfällt“; `null` entfernt das Ereignis. */
internal fun TalkSlot.withWeekEvent(event: WeekEvent?): TalkSlot = if (event == null) {
    copy(weekEventId = null, weekEventName = null)
} else {
    copy(weekEventId = event.id, weekEventName = event.name, talkCancelled = event.cancelsTalk)
}

/** Nummer und Titel des Vortrags als Momentaufnahme; `null` entfernt ihn, ein inaktiver Vortrag ändert nichts. */
internal fun TalkSlot.withTalk(talk: Talk?): TalkSlot =
    if (talk != null && !talk.active) this else copy(talkNumber = talk?.number, talkTitle = talk?.title)

/** Ein frei eingetragener Vortrag, der nicht in der Liste steht: nur der Titel, ohne Nummer. */
internal fun TalkSlot.withFreeTalk(title: String): TalkSlot = copy(talkNumber = null, talkTitle = title)

/** Ein frei eingetragener Redner, der in keiner Liste steht: nur der Name, ohne ID und Versammlung. */
internal fun TalkSlot.withFreeSpeaker(name: String): TalkSlot = copy(
    speakerMemberId = null,
    speakerLastName = name,
    speakerFirstName = "",
    externalSpeakerId = null,
    speakerCongregation = null,
)

/** Ein Mitglied als Redner; `null`, wenn es schon Vorsitzender ist. */
internal fun TalkSlot.withSpeaker(speaker: Speaker?): TalkSlot? = when {
    speaker != null && speaker.memberId == chairmanMemberId -> null
    else -> copy(
        speakerMemberId = speaker?.memberId,
        speakerLastName = speaker?.lastName,
        speakerFirstName = speaker?.firstName,
        externalSpeakerId = null,
        speakerCongregation = null,
    )
}

/** Ein externer Redner samt seiner Versammlung; ersetzt ein Mitglied als Redner. */
internal fun TalkSlot.withExternalSpeaker(speaker: ExternalSpeaker?): TalkSlot = copy(
    speakerMemberId = null,
    speakerLastName = speaker?.lastName,
    speakerFirstName = speaker?.firstName,
    externalSpeakerId = speaker?.id,
    speakerCongregation = speaker?.congregationName,
)

/** Ein Mitglied als Vorsitzender; `null`, wenn es schon Redner ist. */
internal fun TalkSlot.withChairman(chairman: Chairman?): TalkSlot? = when {
    chairman != null && chairman.memberId == speakerMemberId -> null
    else -> copy(
        chairmanMemberId = chairman?.memberId,
        chairmanLastName = chairman?.lastName,
        chairmanFirstName = chairman?.firstName,
    )
}
