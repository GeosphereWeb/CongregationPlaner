package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import kotlinx.datetime.LocalDate

/** The talks that were held [count] times; `0` is the group of talks that were never held. */
data class FrequencyGroup(val count: Int, val rows: List<TalkHistoryRow>) {
    val title: String get() = if (count == 0) "Noch nie gehalten" else "$count× gehalten"
}

/** Talks grouped by how often they were held: most often first, never held last; by number within a group. */
internal fun List<TalkHistoryRow>.groupedByFrequency(): List<FrequencyGroup> = groupBy { it.entry?.heldOn?.size ?: 0 }
    .map { (count, rows) -> FrequencyGroup(count, rows.sortedBy { it.talk.number }) }
    .sortedWith(compareBy({ it.count == 0 }, { -it.count }))

/** The days [speaker]'s talk [talkNumber] was held, newest first; [talk] is `null` if it left the catalog. */
data class SpeakerTalkDays(val talkNumber: Int, val talk: Talk?, val days: List<LocalDate>)

/** What one speaker held: his talks by number with the days. */
data class SpeakerHistory(val speaker: String, val talks: List<SpeakerTalkDays>)

/**
 * The speakers whose name contains [query] (from [MIN_SEARCH_LENGTH] letters, ignoring case), each with the talks they
 * held and when. Speakers come from the names stored in the history, so members and guests are found alike.
 */
internal fun searchSpeakerHistory(
    talks: List<Talk>,
    history: List<TalkHistoryEntry>,
    query: String,
): List<SpeakerHistory> {
    val search = query.trim().takeIf { it.length >= MIN_SEARCH_LENGTH } ?: return emptyList()
    val byNumber = talks.associateBy { it.number }
    val heldBySpeaker = linkedMapOf<String, MutableList<SpeakerTalkDays>>()
    for (entry in history) {
        entry.speakers.entries.filter { it.value.contains(search, ignoreCase = true) }
            .groupBy({ it.value }, { it.key })
            .forEach { (speaker, days) ->
                heldBySpeaker.getOrPut(speaker) { mutableListOf() } +=
                    SpeakerTalkDays(entry.talkNumber, byNumber[entry.talkNumber], days.sortedDescending())
            }
    }
    return heldBySpeaker.map { (speaker, held) -> SpeakerHistory(speaker, held.sortedBy { it.talkNumber }) }
        .sortedBy { it.speaker.lowercase() }
}
