package de.geosphere.congregationplaner.home.domain.model

import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.event.domain.model.SpecialEvent
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.datetime.LocalDate

/**
 * Was die Startseite zeigt. `null` bedeutet: Der Nutzer darf den Block nicht sehen, die Daten wurden deshalb auch
 * nicht abgefragt (die Rules würden sie ablehnen).
 *
 * @param talk Block für Vortragsplaner.
 * @param pendingApprovals Anzahl der Mitglieder mit Konto, die auf ihre Freigabe warten (Block für Versammlungsadmins).
 * @param events „Besondere Ereignisse“: die kommenden Ereignisse für die Gruppen des Mitglieds und die Termine mit
 *   Wochenereignis im Zeitraum der Versammlung (ADR-0037), nach Beginn sortiert.
 */
data class HomeSummary(
    val talk: TalkSummary? = null,
    val pendingApprovals: Int? = null,
    val events: List<HomeEvent> = emptyList(),
)

/**
 * Ein Eintrag unter „Besondere Ereignisse“: ein Ereignis des Versammlungsadmins oder ([fromSchedule]) ein Termin der
 * Vortragsplanung mit Wochenereignis. [icon] ist der Ressourcenname des Icons der Ereignisart (ADR-0036).
 */
data class HomeEvent(
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate = startDate,
    val icon: String? = null,
    val fromSchedule: Boolean = false,
) {
    val isSingleDay: Boolean get() = startDate == endDate
}

/** Führt beide Quellen zusammen; Titel und Icon eines Termins kommen aus seiner Ereignisart, sonst aus dem Termin. */
internal fun homeEvents(events: List<SpecialEvent>, slots: List<TalkSlot>, types: List<EventType>): List<HomeEvent> {
    val byId = types.associateBy { it.id }
    val special = events.map { HomeEvent(it.title, it.startDate, it.endDate, it.eventTypeId?.let(byId::get)?.icon) }
    val schedule = slots.map { slot ->
        val type = slot.weekEventId?.let(byId::get)
        HomeEvent(type?.name ?: slot.weekEventName.orEmpty(), slot.date, icon = type?.icon, fromSchedule = true)
    }
    return (special + schedule).sortedWith(compareBy<HomeEvent> { it.startDate }.thenBy { it.title.lowercase() })
}

/**
 * @param nextSlot Der erste Termin ab heute, an dem ein Vortrag stattfindet; `null`, wenn es keinen gibt.
 * @param openDates Die Termine in den nächsten [HORIZON_DAYS] Tagen, denen ein Vortrag oder ein Redner fehlt,
 *   aufsteigend nach Datum. Abgesagte Termine zählen nicht.
 * @param asHelper `true`, wenn der Nutzer nur als Vortragsplanungs-Helfer sieht und nicht selbst plant.
 */
data class TalkSummary(
    val nextSlot: TalkSlot? = null,
    val openDates: List<LocalDate> = emptyList(),
    val asHelper: Boolean = false,
) {
    companion object {
        /** Vier Wochen. */
        const val HORIZON_DAYS = 28
    }
}
