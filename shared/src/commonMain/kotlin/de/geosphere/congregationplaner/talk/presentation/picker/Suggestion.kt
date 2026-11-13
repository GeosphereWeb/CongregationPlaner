package de.geosphere.congregationplaner.talk.presentation.picker

import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.MemberRef
import de.geosphere.congregationplaner.talk.domain.model.PlanningUsage
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.presentation.common.toDisplayText
import kotlinx.datetime.LocalDate

/** Ein Eintrag im Auswahldialog mit dem Tag, an dem er zuletzt eingeplant ist, und ob ein Stichtag ihn sperrt. */
internal data class Suggestion<T>(val item: T, val lastPlanned: LocalDate?, val blocked: Boolean) {
    /** Hinweis unter dem Namen, z. B. „zuletzt eingeplant So, 02.11.2025“. */
    val note: String
        get() = when {
            lastPlanned == null -> "noch nie eingeplant"
            blocked -> "gesperrt – eingeplant ${lastPlanned.toDisplayText()}"
            else -> "zuletzt eingeplant ${lastPlanned.toDisplayText()}"
        }
}

/**
 * Die Vorschläge: zuerst die freien Einträge, die am längsten nicht eingeplant sind (nie eingeplante ganz oben), dann
 * mit [showBlocked] die gesperrten. Bei gleichem Tag bleibt die Reihenfolge der Liste (Name bzw. Nummer).
 */
internal fun <T> List<T>.suggested(
    lastPlanned: (T) -> LocalDate?,
    blocked: (T) -> Boolean,
    showBlocked: Boolean,
): List<Suggestion<T>> = map { Suggestion(it, lastPlanned(it), blocked(it)) }
    .filter { showBlocked || !it.blocked }
    .sortedWith(compareBy({ it.blocked }, { it.lastPlanned }))

internal fun List<Talk>.suggested(usage: PlanningUsage, showBlocked: Boolean): List<Suggestion<Talk>> =
    suggested({ usage.talkLastPlanned[it.number] }, { usage.isTalkBlocked(it.number) }, showBlocked)

internal fun List<MemberRef>.suggestedMembers(usage: PlanningUsage, showBlocked: Boolean): List<Suggestion<MemberRef>> =
    suggested({ usage.memberLastPlanned[it.memberId] }, { usage.isMemberBlocked(it.memberId) }, showBlocked)

internal fun List<ExternalSpeaker>.suggestedExternal(
    usage: PlanningUsage,
    showBlocked: Boolean,
): List<Suggestion<ExternalSpeaker>> =
    suggested({ usage.externalLastPlanned[it.id] }, { usage.isExternalBlocked(it.id) }, showBlocked)

/** Externe Redner, die den Vortrag [talkNumber] halten; wer keine Vorträge hinterlegt hat, bleibt immer wählbar. */
internal fun List<ExternalSpeaker>.offering(talkNumber: Int?): List<ExternalSpeaker> =
    filter { talkNumber.isOfferedIn(it.talkNumbers) }

/** Heimische Redner, die den Vortrag [talkNumber] halten; wer keine Vorträge hinterlegt hat, bleibt immer wählbar. */
internal fun List<MemberRef>.offeringMembers(talkNumber: Int?): List<MemberRef> =
    filter { talkNumber.isOfferedIn(it.talkNumbers) }

private fun Int?.isOfferedIn(talkNumbers: List<Int>): Boolean =
    this == null || talkNumbers.isEmpty() || this in talkNumbers

/** Die Vorträge mit den Nummern [talkNumbers]; ohne Redner (`null`) oder ohne hinterlegte Vorträge alle. */
internal fun List<Talk>.offeredBy(talkNumbers: List<Int>?): List<Talk> {
    val numbers = talkNumbers?.takeIf { it.isNotEmpty() } ?: return this
    return filter { it.number in numbers }
}

/** Der im Termin eingeteilte externe Redner aus der Liste; `null` ohne externen Redner oder wenn er fehlt. */
internal fun TalkSlot.selectedExternal(speakers: List<ExternalSpeaker>): ExternalSpeaker? =
    externalSpeakerId?.let { id -> speakers.firstOrNull { it.id == id } }

/** Die Vorträge des im Termin eingeteilten Redners, heimisch oder extern; `null` ohne (bekannten) Redner. */
internal fun TalkSlot.selectedSpeakerTalks(members: List<MemberRef>, externals: List<ExternalSpeaker>): List<Int>? =
    speakerMemberId?.let { id -> members.firstOrNull { it.memberId == id }?.talkNumbers }
        ?: selectedExternal(externals)?.talkNumbers
