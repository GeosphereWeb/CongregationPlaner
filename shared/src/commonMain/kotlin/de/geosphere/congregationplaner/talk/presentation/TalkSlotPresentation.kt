package de.geosphere.congregationplaner.talk.presentation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import de.geosphere.congregationplaner.presentation.components.CardHighlight
import de.geosphere.congregationplaner.presentation.navigation.germanMonthName
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.Speaker
import de.geosphere.congregationplaner.talk.domain.model.SpeakerContact
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.upcomingTalkSlots
import de.geosphere.congregationplaner.ui.chip.StatusTone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber

/**
 * Wo ein Termin der Vortragsplanung steht; bestimmt die Marke in der Liste und im Detail. Vortrag und Redner werden
 * immer zusammen geplant: fehlt eines davon, ist der Termin [NOT_PLANNED]. [INFORMED] heißt, der Redner wurde an den
 * Termin erinnert (Feld `confirmed`). [NEXT] ist nur eine Marke, kein eigener Planungsstand.
 */
enum class SlotStatus {
    NEXT,
    CANCELLED,
    NOT_PLANNED,
    PLANNED,
    INFORMED,
}

/**
 * Die Stände, nach denen die Liste gezählt und gefiltert wird, in der Reihenfolge der Anzeige. „Informiert“ ist kein
 * eigener Filter: informierte Termine zählen zu „Geplant“ (siehe [filterStatus]).
 */
internal val FILTER_STATUSES = listOf(SlotStatus.NOT_PLANNED, SlotStatus.PLANNED, SlotStatus.CANCELLED)

/** Planungsstand ohne die Marke „Nächster“. */
internal fun planningStatus(slot: TalkSlot): SlotStatus = slotStatus(slot, nextSlotDate = null)

/** Der Filter, zu dem ein Termin gehört: wie [planningStatus], nur dass „Informiert“ zu „Geplant“ zählt. */
internal fun filterStatus(slot: TalkSlot): SlotStatus =
    planningStatus(slot).let { if (it == SlotStatus.INFORMED) SlotStatus.PLANNED else it }

/** Wie viele Termine es je Stand in [FILTER_STATUSES] gibt; Stände ohne Termin fehlen in der Map. */
internal fun statusCounts(slots: List<TalkSlot>): Map<SlotStatus, Int> = slots.groupingBy(::filterStatus).eachCount()

/** Termine, deren Stand in [filter] ist; ein leerer Filter zeigt alle. Mehrere Stände ergänzen sich. */
internal fun filterSlots(slots: List<TalkSlot>, filter: Set<SlotStatus>): List<TalkSlot> =
    if (filter.isEmpty()) slots else slots.filter { filterStatus(it) in filter }

internal fun statusLabel(status: SlotStatus): String = when (status) {
    SlotStatus.NEXT -> "Nächster"
    SlotStatus.CANCELLED -> "Entfällt"
    SlotStatus.NOT_PLANNED -> "Nicht geplant"
    SlotStatus.PLANNED -> "Geplant"
    SlotStatus.INFORMED -> "Informiert"
}

/** Text und Farbe der Statusmarke eines Termins. */
internal data class SlotChip(val text: String, val tone: StatusTone)

/** Der erste Termin ab [today], an dem ein Vortrag gehalten wird; abgesagte Termine zählen nicht. */
internal fun nextSlotDate(slots: List<TalkSlot>, today: LocalDate): LocalDate? =
    slots.upcomingTalkSlots(today).firstOrNull()?.date

/**
 * Marke eines Termins in der Liste: ein geplanter Termin ist der Normalfall und ein nicht geplanter zeigt schon die
 * rote Hervorhebung ([slotHighlight]); beide bekommen keine Marke.
 */
internal fun listSlotChip(slot: TalkSlot, status: SlotStatus): SlotChip? =
    if (status == SlotStatus.PLANNED || status == SlotStatus.NOT_PLANNED) null else slotChip(status)

/**
 * Hervorhebung eines Termins in der Liste: der Termin der aktuellen Woche (Info) hat Vorrang vor einem noch nicht
 * geplanten Termin ab heute (Warnung); vergangene Termine werden nicht hervorgehoben.
 */
internal fun slotHighlight(slot: TalkSlot, today: LocalDate): CardHighlight = when {
    isCurrentWeek(slot.date, today) -> CardHighlight.Info
    slot.date >= today && planningStatus(slot) == SlotStatus.NOT_PLANNED -> CardHighlight.Warning
    else -> CardHighlight.None
}

/** True, wenn [date] in derselben Woche (Montag bis Sonntag) wie [today] liegt. */
internal fun isCurrentWeek(date: LocalDate, today: LocalDate): Boolean = weekStartDays(date) == weekStartDays(today)

private fun weekStartDays(date: LocalDate): Long = date.toEpochDays() - (date.dayOfWeek.isoDayNumber - 1)

/**
 * Reihenfolge der Prüfung: abgesagt, nächster Termin, dann nach Vollständigkeit. „Nächster“ gilt damit nur für einen
 * Termin, an dem ein Vortrag stattfindet.
 */
internal fun slotStatus(slot: TalkSlot, nextSlotDate: LocalDate?): SlotStatus = when {
    slot.talkCancelled -> SlotStatus.CANCELLED
    slot.date == nextSlotDate -> SlotStatus.NEXT
    slot.talkNumber == null || !slot.hasSpeaker -> SlotStatus.NOT_PLANNED
    slot.confirmed -> SlotStatus.INFORMED
    else -> SlotStatus.PLANNED
}

/** Text und Farbe der Statusmarke; der Name des Wochenereignisses steht nie in der Marke, sondern in [slotEventLine]. */
internal fun slotChip(status: SlotStatus): SlotChip = when (status) {
    SlotStatus.NEXT -> SlotChip(statusLabel(status), StatusTone.Gold)
    SlotStatus.CANCELLED -> SlotChip(statusLabel(status), StatusTone.Sky)
    SlotStatus.NOT_PLANNED -> SlotChip(statusLabel(status), StatusTone.Coral)
    SlotStatus.PLANNED -> SlotChip(statusLabel(status), StatusTone.Sky)
    SlotStatus.INFORMED -> SlotChip(statusLabel(status), StatusTone.Mint)
}

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

/** Die häufigste Uhrzeit der [slots]; bei Gleichstand die früheste, ohne Termine `null`. */
internal fun commonSlotTime(slots: List<TalkSlot>): LocalTime? = slots.groupingBy { it.time }.eachCount().entries
    .sortedWith(compareBy({ -it.value }, { it.key }))
    .firstOrNull()?.key
