package de.geosphere.congregationplaner.home.domain.model

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.datetime.LocalDate

/**
 * Was die Startseite zeigt. `null` bedeutet: Der Nutzer darf den Block nicht sehen, die Daten wurden deshalb auch
 * nicht abgefragt (die Rules würden sie ablehnen).
 *
 * @param talk Block für Vortragsplaner.
 * @param pendingApprovals Anzahl der Mitglieder mit Konto, die auf ihre Freigabe warten (Block für Versammlungsadmins).
 */
data class HomeSummary(val talk: TalkSummary? = null, val pendingApprovals: Int? = null)

/**
 * @param nextSlot Der erste Termin ab heute, an dem ein Vortrag stattfindet; `null`, wenn es keinen gibt.
 * @param openDates Die Termine in den nächsten [HORIZON_DAYS] Tagen, denen ein Vortrag oder ein Redner fehlt,
 *   aufsteigend nach Datum. Abgesagte Termine zählen nicht.
 */
data class TalkSummary(val nextSlot: TalkSlot? = null, val openDates: List<LocalDate> = emptyList()) {
    companion object {
        /** Vier Wochen. */
        const val HORIZON_DAYS = 28
    }
}
