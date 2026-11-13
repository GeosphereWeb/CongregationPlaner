package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import dev.zacsweers.metro.Inject
import kotlinx.datetime.LocalDate

/** Ein Vortrag der Vortragsliste mit seiner Historie; [entry] ist `null`, wenn er noch nie gehalten wurde. */
data class TalkHistoryRow(val talk: Talk, val entry: TalkHistoryEntry?)

/** Die Vorträge, die [count]-mal gehalten wurden; `0` ist die Gruppe der nie gehaltenen. */
data class FrequencyGroup(val count: Int, val rows: List<TalkHistoryRow>)

/** Die Übersicht der Historie: [rows] nach Nummer, [groups] nach Häufigkeit. */
data class TalkHistoryOverview(
    val rows: List<TalkHistoryRow> = emptyList(),
    val groups: List<FrequencyGroup> = emptyList(),
) {
    val neverHeldCount: Int get() = rows.count { it.entry == null }
}

/**
 * Ordnet jedem Vortrag der Liste seine Historie zu (nach Nummer) und gruppiert nach Häufigkeit: häufigste zuerst, nie
 * gehaltene zuletzt, innerhalb einer Gruppe nach Nummer.
 */
@Inject
class BuildTalkHistoryOverviewUseCase {
    operator fun invoke(talks: List<Talk>, history: List<TalkHistoryEntry>): TalkHistoryOverview {
        val byNumber = history.associateBy { it.talkNumber }
        val rows = talks.sortedBy { it.number }.map { TalkHistoryRow(it, byNumber[it.number]) }
        val groups = rows.groupBy { it.entry?.heldOn?.size ?: 0 }
            .map { (count, grouped) -> FrequencyGroup(count, grouped.sortedBy { it.talk.number }) }
            .sortedWith(compareBy({ it.count == 0 }, { -it.count }))
        return TalkHistoryOverview(rows, groups)
    }
}

/**
 * Die Tage, an denen der Redner den Vortrag [talkNumber] gehalten hat, neueste zuerst; [talk] fehlt, wenn er nicht mehr
 * in der Liste steht.
 */
data class SpeakerTalkDays(val talkNumber: Int, val talk: Talk?, val days: List<LocalDate>)

/** Was ein Redner gehalten hat: seine Vorträge nach Nummer mit den Tagen. */
data class SpeakerHistory(val speaker: String, val talks: List<SpeakerTalkDays>)

/**
 * Die Redner, deren Name [query] enthält (ab [MIN_LENGTH] Zeichen, ohne Groß-/Kleinschreibung), mit ihren Vorträgen
 * und Tagen. Die Namen stammen aus der Historie; Mitglieder und Gäste werden gleich gefunden.
 */
@Inject
class SearchSpeakerHistoryUseCase {
    operator fun invoke(talks: List<Talk>, history: List<TalkHistoryEntry>, query: String): List<SpeakerHistory> {
        val search = query.trim().takeIf { isActive(it) } ?: return emptyList()
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

    /** Die Suche läuft erst ab [MIN_LENGTH] Zeichen. */
    fun isActive(query: String): Boolean = query.trim().length >= MIN_LENGTH

    companion object {
        const val MIN_LENGTH = 2
    }
}
