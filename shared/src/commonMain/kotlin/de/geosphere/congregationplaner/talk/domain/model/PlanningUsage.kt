package de.geosphere.congregationplaner.talk.domain.model

import kotlinx.datetime.LocalDate

/**
 * Wann Redner und Vorträge zuletzt eingeplant sind (über alle Termine, vergangen oder zukünftig) und welche davon die
 * Stichtage aus [settings] sperren: gesperrt ist, wer an einem Tag ab dem Stichtag eingeplant ist.
 *
 * Mitglieder und externe Redner zählen über ihre ID, Vorträge über ihre Nummer; frei eingetragene Redner und
 * Vorträge haben keine und werden daher weder gezählt noch gesperrt.
 */
data class PlanningUsage(
    val settings: TalkPlanningSettings = TalkPlanningSettings(),
    val memberLastPlanned: Map<String, LocalDate> = emptyMap(),
    val externalLastPlanned: Map<String, LocalDate> = emptyMap(),
    val talkLastPlanned: Map<Int, LocalDate> = emptyMap(),
) {
    fun isMemberBlocked(memberId: String): Boolean = blocks(memberLastPlanned[memberId], settings.speakerFreeSince)

    fun isExternalBlocked(id: String): Boolean = blocks(externalLastPlanned[id], settings.speakerFreeSince)

    fun isTalkBlocked(number: Int): Boolean = blocks(talkLastPlanned[number], settings.talkFreeSince)

    private fun blocks(lastPlanned: LocalDate?, freeSince: LocalDate?): Boolean =
        lastPlanned != null && freeSince != null && lastPlanned >= freeSince
}

/**
 * Die Nutzung aus allen Terminen [slots]. Der Termin am [excludedDate] (der gerade bearbeitete) zählt nicht mit,
 * damit seine eigene Belegung ihn nicht sperrt; abgesagte Vorträge zählen weder für den Vortrag noch für den Redner.
 */
fun planningUsageOf(slots: List<TalkSlot>, settings: TalkPlanningSettings, excludedDate: LocalDate?): PlanningUsage {
    val held = slots.filter { !it.talkCancelled && it.date != excludedDate }
    return PlanningUsage(
        settings = settings,
        memberLastPlanned = held.latestBy { it.speakerMemberId },
        externalLastPlanned = held.latestBy { it.externalSpeakerId },
        talkLastPlanned = held.latestBy { it.talkNumber },
    )
}

private fun <K> List<TalkSlot>.latestBy(key: (TalkSlot) -> K?): Map<K, LocalDate> = mapNotNull { slot ->
    key(slot)?.let { it to slot.date }
}.groupBy({ it.first }, { it.second }).mapValues { (_, dates) -> dates.max() }
