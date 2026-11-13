package de.geosphere.congregationplaner.talk.domain.model

/** Wen ein Termin bisher nennt: einen externen Redner oder ein Mitglied (als Redner oder Vorsitzender). */
sealed interface SlotPerson {
    data class External(val id: String) : SlotPerson

    data class Member(val id: String) : SlotPerson
}

/**
 * Der Termin, in dem [to] an die Stelle von [from] tritt: als Redner (ein externer Redner wird dabei zum Mitglied) und,
 * bei einem Mitglied, auch als Vorsitzender. Nennt der Termin [from] nicht, bleibt er unverändert.
 */
fun TalkSlot.withPersonReplaced(from: SlotPerson, to: MemberRef): TalkSlot {
    val isSpeaker = when (from) {
        is SlotPerson.External -> externalSpeakerId == from.id
        is SlotPerson.Member -> speakerMemberId == from.id
    }
    val isChairman = from is SlotPerson.Member && chairmanMemberId == from.id
    var slot = this
    if (isSpeaker) {
        slot = slot.copy(
            speakerMemberId = to.memberId,
            speakerLastName = to.lastName,
            speakerFirstName = to.firstName,
            externalSpeakerId = null,
            speakerCongregation = null,
        )
    }
    if (isChairman) {
        slot =
            slot.copy(chairmanMemberId = to.memberId, chairmanLastName = to.lastName, chairmanFirstName = to.firstName)
    }
    return slot
}
