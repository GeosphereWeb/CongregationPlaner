package de.geosphere.congregationplaner.talk.domain.model

/** `true`, wenn am Termin ein Vortrag oder ein Redner eingeplant ist, also etwas, das sich umplanen lässt. */
val TalkSlot.hasAssignment: Boolean get() = hasTalk || hasSpeaker

/**
 * Plant die Belegung von [from] (Vortrag, Redner, Bestätigung) auf [to] um und gibt [from] die bisherige Belegung von
 * [to]; ist [to] frei, wird [from] frei. Ereignis, Uhrzeit, „Vortrag entfällt“, Vorsitz und Notiz bleiben am Tag.
 * Liefert die neuen Termine in der Reihenfolge ([from], [to]).
 */
fun reassignTalk(from: TalkSlot, to: TalkSlot): Pair<TalkSlot, TalkSlot> =
    from.withAssignmentOf(to) to to.withAssignmentOf(from)

private fun TalkSlot.withAssignmentOf(other: TalkSlot): TalkSlot = copy(
    talkNumber = other.talkNumber,
    talkTitle = other.talkTitle,
    speakerMemberId = other.speakerMemberId,
    speakerLastName = other.speakerLastName,
    speakerFirstName = other.speakerFirstName,
    externalSpeakerId = other.externalSpeakerId,
    speakerCongregation = other.speakerCongregation,
    confirmed = other.confirmed,
)
