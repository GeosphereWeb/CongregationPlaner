package de.geosphere.congregationplaner.talk.presentation.schedule

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import de.geosphere.congregationplaner.presentation.navigation.germanMonthName
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.Speaker
import de.geosphere.congregationplaner.talk.domain.model.SpeakerContact
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.datetime.LocalDate

/** Name des Wochenereignisses als Zeile über dem Titel in der Liste; nur dort steht er. */
internal fun slotEventLine(slot: TalkSlot): String? = slot.weekEventName

/** „Nr. 12 – Thema“; ohne gespeichertes Thema nur die Nummer. */
internal fun slotTitle(slot: TalkSlot): String = slot.talkNumber?.let { number ->
    slot.talkTitle?.takeIf { it.isNotBlank() }?.let { "Nr. $number – $it" } ?: "Nr. $number"
} ?: "Noch nicht geplant"

/**
 * Titel in der Liste mit fetter Vortragsnummer („**Nr. 34** – Thema“); an einem entfallenden Termin ist nichts geplant,
 * also steht dort nichts.
 */
internal fun slotListTitle(slot: TalkSlot): AnnotatedString? {
    if (slot.talkCancelled) return null
    val boldLength = slot.talkNumber?.let { "Nr. $it".length } ?: 0
    val bold = AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold), 0, boldLength)
    return AnnotatedString(slotTitle(slot), listOf(bold))
}

/** Redner in der Liste; an einem entfallenden Termin steht dort nichts (siehe [slotListTitle]). */
/** „Informiert“ lässt sich nur für einen Termin mit Vortrag und Redner setzen, der nicht entfällt. */
internal val TalkSlot.canBeInformed: Boolean
    get() = talkNumber != null && (speaker != null || externalSpeaker != null) && !talkCancelled

internal fun slotListSpeakerLine(slot: TalkSlot): String? = slotSpeakerLine(slot).takeUnless { slot.talkCancelled }

/**
 * Wie sich der eingeteilte Redner erreichen lässt; `null` ohne Redner, ohne seine Nummern in [speakers] bzw.
 * [externalSpeakers] (fehlende Berechtigung) oder ohne wählbare Nummer.
 */
internal fun slotContact(
    slot: TalkSlot,
    speakers: List<Speaker>,
    externalSpeakers: List<ExternalSpeaker>,
): SpeakerContact? {
    val contact = when {
        slot.externalSpeakerId != null ->
            externalSpeakers.firstOrNull { it.id == slot.externalSpeakerId }
                ?.let { SpeakerContact(it.mobilePhone, it.landlinePhone) }
        slot.speakerMemberId != null ->
            speakers.firstOrNull { it.memberId == slot.speakerMemberId }
                ?.let { SpeakerContact(it.mobilePhone, it.landlinePhone) }
        else -> null
    }
    return contact?.takeIf { it.isReachable }
}

/** Redner mit Herkunft: „Muster, Max · eigene Versammlung“, „Extern, Eva · Nord“ oder „Redner noch offen“. */
internal fun slotSpeakerLine(slot: TalkSlot): String {
    val external = slot.externalSpeaker
    val member = slot.speaker
    return when {
        external != null -> "${personName(slot)} · ${external.congregationName}"
        member != null -> "${member.displayName} · eigene Versammlung"
        else -> "Redner noch offen"
    }
}

private fun personName(slot: TalkSlot): String =
    listOf(slot.speakerLastName, slot.speakerFirstName).filter { !it.isNullOrBlank() }.joinToString(", ")

/** Überschrift einer Monatsgruppe der Liste, z. B. „November 2026“. */
internal fun slotMonthLabel(date: LocalDate): String = "${germanMonthName(date)} ${date.year}"

/** Datum für die Überschrift des Detailbereichs, z. B. „1. November 2026“. */
internal fun slotDateHeadline(date: LocalDate): String = "${date.day}. ${germanMonthName(date)} ${date.year}"
