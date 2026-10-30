package de.geosphere.congregationplaner.talk.domain.model

import kotlinx.datetime.LocalDate

/**
 * Wann ein Vortrag in der Versammlung gehalten wurde, aus `…/congregation/{id}/talkHistory/{Vortragsnummer}`.
 * Die Vortragsnummer ist zugleich die Dokument-ID; ein Vortrag ohne Eintrag wurde noch nie gehalten.
 *
 * @param heldOn Die Tage, an denen der Vortrag gehalten wurde; aufsteigend, ohne Duplikate und nie leer.
 * @param plannedOn Die Tage aus [heldOn], die aus der Planung stammen. Sie ändern sich nur dort und sind in der
 *   Historie nicht änderbar; die übrigen Tage sind nachgetragen.
 * @param speakers Der Redner je Tag aus [heldOn] als Momentaufnahme des Anzeigenamens aus der Planung; Tage ohne
 *   bekannten Redner fehlen.
 */
data class TalkHistoryEntry(
    val talkNumber: Int,
    val heldOn: List<LocalDate>,
    val plannedOn: List<LocalDate> = emptyList(),
    val speakers: Map<LocalDate, String> = emptyMap(),
) {
    /** Der Tag, an dem der Vortrag zuletzt gehalten wurde. */
    val lastHeldOn: LocalDate? get() = heldOn.maxOrNull()

    companion object {
        const val MAX_DATES = 100
    }
}
