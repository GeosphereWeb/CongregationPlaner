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
 */
data class TalkSlot(
    val date: LocalDate,
    val time: LocalTime,
    val weekEventId: String? = null,
    val weekEventName: String? = null,
    val talkCancelled: Boolean = false,
) {
    /** Dokument-ID in Firestore: ISO-Datum `yyyy-MM-dd`. */
    val id: String get() = date.toString()

    companion object {
        const val MAX_WEEK_EVENT_NAME_LENGTH = 100
    }
}

/** Ein Wochenereignis (z. B. Kreisaufseherbesuch); die Liste gilt für alle Versammlungen und Kreise. */
data class WeekEvent(val id: String, val name: String, val cancelsTalk: Boolean = false)

sealed class TalkScheduleException(message: String) : Exception(message) {
    class NotAllowed : TalkScheduleException("Du darfst die Vortragsplanung nicht bearbeiten.")

    class InvalidWeekEvent :
        TalkScheduleException(
            "Das Wochenereignis ist ungültig (höchstens ${TalkSlot.MAX_WEEK_EVENT_NAME_LENGTH} Zeichen).",
        )
}
