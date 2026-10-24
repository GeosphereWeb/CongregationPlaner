package de.geosphere.congregationplaner.talk.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Ein Versammlungstag in der Vortragsplanung aus `…/congregation/{id}/talkSchedule/{yyyy-MM-dd}`.
 * Das Datum ist zugleich die Dokument-ID.
 *
 * @param weekEventId Das Wochenereignis aus der globalen Liste; der Name wird mitgespeichert, damit alte
 *   Termine lesbar bleiben, wenn die Liste sich ändert.
 * @param talkCancelled `true`, wenn an diesem Tag kein Vortrag gehalten wird; Vorbelegung ist
 *   [WeekEvent.cancelsTalk], der Planer kann sie überschreiben.
 * @param talkNumber Nummer des Vortrags aus der Vortragsliste; der Titel wird mitgespeichert, damit alte
 *   Termine unverändert bleiben, wenn sich der Titel in der Liste ändert. Nummer und Titel gehören zusammen.
 * @param speakerMemberId Das Mitglied der Versammlung, das den Vortrag hält; Name und Vorname werden als
 *   Momentaufnahme mitgespeichert. Die drei Redner-Felder gehören zusammen.
 * @param externalSpeakerId Statt eines Mitglieds ein externer Redner aus der Liste der Versammlung; Name, Vorname
 *   und [speakerCongregation] (seine Versammlung) sind eine Momentaufnahme. Ein Termin hat höchstens eines von
 *   [speakerMemberId] und [externalSpeakerId].
 * @param chairmanMemberId Das Mitglied, das die Zusammenkunft leitet (Vorsitzender); Name und Vorname werden
 *   wie beim Redner als Momentaufnahme mitgespeichert, die drei Felder gehören zusammen.
 */
data class TalkSlot(
    val date: LocalDate,
    val time: LocalTime,
    val weekEventId: String? = null,
    val weekEventName: String? = null,
    val talkCancelled: Boolean = false,
    val talkNumber: Int? = null,
    val talkTitle: String? = null,
    val speakerMemberId: String? = null,
    val speakerLastName: String? = null,
    val speakerFirstName: String? = null,
    val chairmanMemberId: String? = null,
    val chairmanLastName: String? = null,
    val chairmanFirstName: String? = null,
    val externalSpeakerId: String? = null,
    val speakerCongregation: String? = null,
) {
    /** Dokument-ID in Firestore: ISO-Datum `yyyy-MM-dd`. */
    val id: String get() = date.toString()

    val speaker: Speaker?
        get() = speakerMemberId?.let { Speaker(it, speakerLastName.orEmpty(), speakerFirstName.orEmpty()) }

    val externalSpeaker: ExternalSpeaker?
        get() = externalSpeakerId?.let {
            ExternalSpeaker(it, speakerLastName.orEmpty(), speakerFirstName.orEmpty(), speakerCongregation.orEmpty())
        }

    val chairman: Chairman?
        get() = chairmanMemberId?.let { Chairman(it, chairmanLastName.orEmpty(), chairmanFirstName.orEmpty()) }

    companion object {
        const val MAX_WEEK_EVENT_NAME_LENGTH = 100
        const val MAX_TALK_TITLE_LENGTH = 200
        const val MAX_PERSON_NAME_LENGTH = 100
    }
}

/**
 * Ein Vortrag aus der Vortragsliste (`talkCatalog/{Nummer}`); sie gilt für alle Versammlungen und Kreise.
 * Die Nummer bleibt stabil, der Titel kann sich ändern, und ein Vortrag kann zeitweise nicht [active] sein.
 */
data class Talk(val number: Int, val title: String, val active: Boolean = true) {
    companion object {
        const val MAX_NUMBER = 9999
    }
}

/** Ein Mitglied der Versammlung, wie es als Redner oder Vorsitzender im Termin ausgewählt wird. */
data class MemberRef(val memberId: String, val lastName: String, val firstName: String) {
    val displayName: String get() = listOf(lastName, firstName).filter(String::isNotBlank).joinToString(", ")
}

/**
 * Ein Redner aus einer anderen Versammlung, den ein Planer in der Liste seiner Versammlung (`externalSpeakers/{id}`) pflegt.
 * [id] ist leer, solange er noch nicht gespeichert ist.
 */
data class ExternalSpeaker(val id: String, val lastName: String, val firstName: String, val congregationName: String) {
    val displayName: String
        get() = listOf(lastName, firstName).filter(String::isNotBlank).joinToString(", ") + " ($congregationName)"
}

/** Mitglieder mit `isSpeaker`. */
typealias Speaker = MemberRef

/** Brüder mit dem Dienstamt Ältester oder Dienstamtgehilfe. */
typealias Chairman = MemberRef

/** Ein Wochenereignis (z. B. Kreisaufseherbesuch); die Liste gilt für alle Versammlungen und Kreise. */
data class WeekEvent(val id: String, val name: String, val cancelsTalk: Boolean = false)

sealed class TalkScheduleException(message: String) : Exception(message) {
    class NotAllowed : TalkScheduleException("Du darfst die Vortragsplanung nicht bearbeiten.")

    class InvalidWeekEvent :
        TalkScheduleException(
            "Das Wochenereignis ist ungültig (höchstens ${TalkSlot.MAX_WEEK_EVENT_NAME_LENGTH} Zeichen).",
        )

    class InvalidSpeaker :
        TalkScheduleException(
            "Der Redner ist ungültig: Familienname erforderlich, Namen mit höchstens " +
                "${TalkSlot.MAX_PERSON_NAME_LENGTH} Zeichen.",
        )

    class InvalidChairman :
        TalkScheduleException(
            "Der Vorsitzende ist ungültig: Familienname erforderlich, Namen mit höchstens " +
                "${TalkSlot.MAX_PERSON_NAME_LENGTH} Zeichen.",
        )

    class SpeakerIsChairman : TalkScheduleException("Redner und Vorsitzender dürfen nicht dieselbe Person sein.")

    class InvalidTalk :
        TalkScheduleException(
            "Der Vortrag ist ungültig: Nummer ab 1 und ein Titel mit höchstens " +
                "${TalkSlot.MAX_TALK_TITLE_LENGTH} Zeichen.",
        )
}
