package de.geosphere.congregationplaner.event.domain.model

import de.geosphere.congregationplaner.user.domain.model.Appointment
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import kotlinx.datetime.LocalDate

/**
 * Ein besonderes Ereignis der Versammlung aus `…/congregation/{id}/events/{id}` (ADR-0033): ein oder mehrere Tage,
 * sichtbar für die Gruppen in [audience]. Eintägige Ereignisse haben [endDate] gleich [startDate]. [eventTypeId]
 * verweist optional auf eine Ereignisart der Versammlung (ADR-0036), von der das Icon kommt.
 */
data class SpecialEvent(
    val id: String = "",
    val title: String = "",
    val startDate: LocalDate,
    val endDate: LocalDate = startDate,
    val audience: Set<EventAudience> = setOf(EventAudience.ALL),
    val eventTypeId: String? = null,
) {
    val isSingleDay: Boolean get() = startDate == endDate

    companion object {
        const val MAX_TITLE_LENGTH = 100
    }
}

/** Werte des Firestore-Felds `audience`: die Gruppen, an die sich ein Ereignis richtet. */
enum class EventAudience(val label: String) {
    ALL("Alle"),
    BROTHERS("Brüder"),
    SISTERS("Schwestern"),
    ELDERS("Älteste"),
    MINISTERIAL_SERVANTS("Dienstamtgehilfen"),
    ;

    companion object {
        /** Unbekannte Werte werden ignoriert (deny by default). */
        fun fromValues(values: Iterable<String?>): Set<EventAudience> =
            values.mapNotNull { value -> entries.firstOrNull { it.name == value } }.toSet()
    }
}

/** Die Gruppen eines Mitglieds; entspricht `eventGroups()` in den Security Rules. */
fun MemberProfile.eventGroups(): Set<EventAudience> = buildSet {
    add(EventAudience.ALL)
    if (isBrother) add(EventAudience.BROTHERS)
    if (isSister) add(EventAudience.SISTERS)
    if (isBrother && appointment == Appointment.ELDER) add(EventAudience.ELDERS)
    if (isBrother && appointment == Appointment.MINISTERIAL_SERVANT) add(EventAudience.MINISTERIAL_SERVANTS)
}

/** Fachliche Fehler beim Pflegen der Ereignisse; die Nachrichten werden direkt in der UI angezeigt. */
sealed class SpecialEventException(message: String) : Exception(message) {
    class NotAllowed : SpecialEventException("Dafür fehlt dir die Berechtigung.")

    class InvalidTitle :
        SpecialEventException("Bitte gib einen Titel mit höchstens ${SpecialEvent.MAX_TITLE_LENGTH} Zeichen ein.")

    class InvalidDates : SpecialEventException("Das Ende darf nicht vor dem Beginn liegen.")

    class MissingAudience : SpecialEventException("Bitte wähle aus, wer das Ereignis sehen soll.")
}
