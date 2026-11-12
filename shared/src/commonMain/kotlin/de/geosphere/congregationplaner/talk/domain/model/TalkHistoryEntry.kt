package de.geosphere.congregationplaner.talk.domain.model

import kotlinx.datetime.LocalDate

/**
 * Wann ein Vortrag in der Versammlung gehalten wurde. Die Historie wird aus den Terminen berechnet ([talkHistoryOf]);
 * ein Vortrag ohne Eintrag wurde noch nie gehalten. Nachgetragen wird ein Tag als Termin in der Vergangenheit.
 *
 * @param heldOn Die Tage, an denen der Vortrag gehalten wurde oder geplant ist; aufsteigend und nie leer.
 * @param speakers Der Redner je Tag aus [heldOn] als Anzeigename des Termins; Tage ohne Redner fehlen.
 */
data class TalkHistoryEntry(
    val talkNumber: Int,
    val heldOn: List<LocalDate>,
    val speakers: Map<LocalDate, String> = emptyMap(),
) {
    /** Der Tag, an dem der Vortrag zuletzt gehalten wurde. */
    val lastHeldOn: LocalDate? get() = heldOn.maxOrNull()
}

/**
 * Die Historie aus den Terminen [slots]: je Vortragsnummer die Tage mit Redner, nach Nummer sortiert. Nur ein Termin
 * mit Vortrag, der nicht abgesagt ist, zählt; ein geplanter Termin in der Zukunft zählt schon mit.
 */
fun talkHistoryOf(slots: List<TalkSlot>): List<TalkHistoryEntry> = slots
    .filter { it.talkNumber != null && !it.talkCancelled }
    .groupBy { requireNotNull(it.talkNumber) }
    .map { (number, held) ->
        TalkHistoryEntry(
            talkNumber = number,
            heldOn = held.map { it.date }.distinct().sorted(),
            speakers = held.mapNotNull { slot ->
                (slot.speaker?.displayName ?: slot.externalSpeaker?.displayName)?.let { slot.date to it }
            }.toMap(),
        )
    }.sortedBy { it.talkNumber }
