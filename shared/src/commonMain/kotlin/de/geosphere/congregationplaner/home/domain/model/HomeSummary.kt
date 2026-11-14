package de.geosphere.congregationplaner.home.domain.model

import de.geosphere.congregationplaner.event.domain.model.SpecialEvent
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.datetime.LocalDate

/**
 * Was die Startseite zeigt. `null` bedeutet: Der Nutzer darf den Block nicht sehen, die Daten wurden deshalb auch
 * nicht abgefragt (die Rules würden sie ablehnen).
 *
 * @param talk Block für Vortragsplaner.
 * @param pendingApprovals Anzahl der Mitglieder mit Konto, die auf ihre Freigabe warten (Block für Versammlungsadmins).
 * @param events Die kommenden besonderen Ereignisse für die Gruppen des Mitglieds, nach Beginn sortiert.
 */
data class HomeSummary(
    val talk: TalkSummary? = null,
    val pendingApprovals: Int? = null,
    val events: List<SpecialEvent> = emptyList(),
)

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
