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
 *   Termine unverändert bleiben, wenn sich der Titel in der Liste ändert. Ein Titel ohne Nummer ist ein frei
 *   eingetragener Vortrag, der nicht in der Liste steht.
 * @param speakerMemberId Das Mitglied der Versammlung, das den Vortrag hält; Name und Vorname werden als
 *   Momentaufnahme mitgespeichert. Die drei Redner-Felder gehören zusammen.
 * @param externalSpeakerId Statt eines Mitglieds ein externer Redner aus der Liste der Versammlung; Name, Vorname
 *   und [speakerCongregation] (seine Versammlung) sind eine Momentaufnahme. Ein Termin hat höchstens eines von
 *   [speakerMemberId] und [externalSpeakerId]. Ein Name ohne beide IDs ist ein frei eingetragener Redner, der in
 *   keiner Liste steht; sein Name steht dann vollständig in [speakerLastName].
 * @param confirmed `true`, wenn der Planer den Termin bestätigt hat (z. B. weil der Redner zugesagt hat); ältere
 *   Termine ohne das Feld gelten als nicht bestätigt.
 * @param note Freie Notiz des Planers zum Termin (höchstens [MAX_NOTE_LENGTH] Zeichen); `null` = keine.
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
    val confirmed: Boolean = false,
    val note: String? = null,
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

    /** `true`, wenn ein Mitglied, ein externer oder ein frei eingetragener Redner eingeteilt ist. */
    val hasSpeaker: Boolean get() = speakerMemberId != null || externalSpeakerId != null || speakerLastName != null

    /** Der frei eingetragene Redner (ohne Mitglied und ohne externen Redner); `null`, wenn es keinen gibt. */
    val freeSpeakerName: String?
        get() = speakerLastName.takeIf { speakerMemberId == null && externalSpeakerId == null }

    /** Anzeigename des Redners, gleich ob Mitglied, externer oder frei eingetragener Redner. */
    val speakerDisplayName: String?
        get() = externalSpeaker?.displayName ?: speaker?.displayName ?: freeSpeakerName

    /** `true`, wenn ein Vortrag aus der Liste oder ein frei eingetragener Vortrag gewählt ist. */
    val hasTalk: Boolean get() = talkNumber != null || talkTitle != null

    /** `true` für einen frei eingetragenen Vortrag: Titel ohne Nummer. */
    val isFreeTalk: Boolean get() = talkNumber == null && talkTitle != null

    companion object {
        const val MAX_WEEK_EVENT_NAME_LENGTH = 100
        const val MAX_TALK_TITLE_LENGTH = 200
        const val MAX_PERSON_NAME_LENGTH = 100
        const val MAX_NOTE_LENGTH = 500
    }
}

/** Der Vortrag als Text: „Nr. 12 – Titel“, ohne Titel nur die Nummer, frei eingetragen nur der Titel; sonst `null`. */
fun TalkSlot.talkLabel(): String? {
    val title = talkTitle?.takeIf { it.isNotBlank() }
    val number = talkNumber ?: return title
    return title?.let { "Nr. $number – $it" } ?: "Nr. $number"
}

/**
 * Ein Vortrag aus der Vortragsliste (`talkCatalog/{Nummer}`); sie gilt für alle Versammlungen und Kreise.
 * Die Nummer bleibt stabil, und ein Vortrag kann zeitweise nicht [active] sein. Der Titel kann sich ändern:
 * [title] ist der ursprüngliche Titel, [titleChanges] sind die späteren Titel mit dem Tag, ab dem sie gelten.
 */
data class Talk(
    val number: Int,
    val title: String,
    val active: Boolean = true,
    val titleChanges: List<TalkTitleChange> = emptyList(),
) {
    /** Der Titel, der am [date] gilt: die letzte Änderung bis dahin, sonst der ursprüngliche [title]. */
    fun titleOn(date: LocalDate): String =
        titleChanges.filter { it.validFrom <= date }.maxByOrNull { it.validFrom }?.title ?: title

    /** Der Vortrag mit dem Titel, der am [date] gilt, z. B. für die Auswahl an einem Termin. */
    fun asOf(date: LocalDate): Talk = copy(title = titleOn(date))

    companion object {
        const val MAX_NUMBER = 9999
        const val MAX_TITLE_CHANGES = 10
    }
}

/** Ein neuer Titel eines Vortrags, der ab [validFrom] gilt. */
data class TalkTitleChange(val validFrom: LocalDate, val title: String)

/** Die Vorträge mit dem Titel, der am [date] gilt. */
fun List<Talk>.asOf(date: LocalDate): List<Talk> = map { it.asOf(date) }

/**
 * Ein Mitglied der Versammlung, wie es als Redner oder Vorsitzender im Termin ausgewählt wird; [talkNumbers] sind die
 * Vorträge eines Redners (leer = unbekannt).
 */
data class MemberRef(
    val memberId: String,
    val lastName: String,
    val firstName: String,
    val mobilePhone: String = "",
    val landlinePhone: String = "",
    val talkNumbers: List<Int> = emptyList(),
) {
    val displayName: String get() = listOf(lastName, firstName).filter(String::isNotBlank).joinToString(", ")
}

/**
 * Ein Redner aus einer anderen Versammlung, den ein Planer in der Liste seiner Versammlung (`externalSpeakers/{id}`)
 * pflegt.
 * [id] ist leer, solange er noch nicht gespeichert ist; [talkNumbers] sind die Vorträge, die er hält (leer =
 * unbekannt).
 */
data class ExternalSpeaker(
    val id: String,
    val lastName: String,
    val firstName: String,
    val congregationName: String,
    val mobilePhone: String = "",
    val landlinePhone: String = "",
    val talkNumbers: List<Int> = emptyList(),
) {
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

    class InvalidNote :
        TalkScheduleException("Die Notiz darf höchstens ${TalkSlot.MAX_NOTE_LENGTH} Zeichen haben.")

    class CongregationNotFound : TalkScheduleException("Die Versammlungsdaten wurden nicht gefunden.")

    class InvalidChairman :
        TalkScheduleException(
            "Der Vorsitzende ist ungültig: Familienname erforderlich, Namen mit höchstens " +
                "${TalkSlot.MAX_PERSON_NAME_LENGTH} Zeichen.",
        )

    class SpeakerIsChairman : TalkScheduleException("Redner und Vorsitzender dürfen nicht dieselbe Person sein.")

    class NothingToReassign : TalkScheduleException("An diesem Termin ist kein Vortrag und kein Redner eingeplant.")

    class TargetCancelled : TalkScheduleException("Am gewählten Tag entfällt der Vortrag.")

    class SameDay : TalkScheduleException("Bitte wähle einen anderen Tag.")

    class InvalidTalk :
        TalkScheduleException(
            "Der Vortrag ist ungültig: Nummer ab 1 und ein Titel mit höchstens " +
                "${TalkSlot.MAX_TALK_TITLE_LENGTH} Zeichen.",
        )
}
