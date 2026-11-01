package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.presentation.navigation.germanMonthName
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.ui.chip.StatusTone
import kotlinx.datetime.LocalDate

/** Wo ein Termin der Vortragsplanung steht; bestimmt die Marke in der Liste und im Detail. */
internal enum class SlotStatus {
    NEXT,
    CANCELLED,
    OPEN,
    SPEAKER_MISSING,
    PLANNED,
    CONFIRMED,
}

/** Text und Farbe der Statusmarke eines Termins. */
internal data class SlotChip(val text: String, val tone: StatusTone)

/** Der erste Termin ab [today], an dem ein Vortrag gehalten wird; abgesagte Termine zählen nicht. */
internal fun nextSlotDate(slots: List<TalkSlot>, today: LocalDate): LocalDate? =
    slots.filter { !it.talkCancelled && it.date >= today }.minOfOrNull { it.date }

/**
 * Reihenfolge der Prüfung: abgesagt, nächster Termin, dann nach Vollständigkeit. „Nächster“ gilt damit nur für einen
 * Termin, an dem ein Vortrag stattfindet.
 */
internal fun slotStatus(slot: TalkSlot, nextSlotDate: LocalDate?): SlotStatus = when {
    slot.talkCancelled -> SlotStatus.CANCELLED
    slot.date == nextSlotDate -> SlotStatus.NEXT
    slot.talkNumber == null -> SlotStatus.OPEN
    slot.speakerMemberId == null && slot.externalSpeakerId == null -> SlotStatus.SPEAKER_MISSING
    slot.confirmed -> SlotStatus.CONFIRMED
    else -> SlotStatus.PLANNED
}

/** Bei einem abgesagten Termin mit Wochenereignis steht dessen Name in der Marke. */
internal fun slotChip(slot: TalkSlot, status: SlotStatus): SlotChip = when (status) {
    SlotStatus.NEXT -> SlotChip("Nächster", StatusTone.Gold)
    SlotStatus.CANCELLED -> SlotChip(slot.weekEventName ?: "Entfällt", StatusTone.Sky)
    SlotStatus.OPEN -> SlotChip("Offen", StatusTone.Coral)
    SlotStatus.SPEAKER_MISSING -> SlotChip("Redner fehlt", StatusTone.Coral)
    SlotStatus.PLANNED -> SlotChip("Geplant", StatusTone.Sky)
    SlotStatus.CONFIRMED -> SlotChip("Bestätigt", StatusTone.Mint)
}

internal fun slotTitle(slot: TalkSlot): String = slot.talkNumber?.let { "Vortrag Nr. $it" } ?: "Noch nicht geplant"

/** Redner mit Herkunft: „Muster, Max · eigene Versammlung“, „Extern, Eva · Gast aus Nord“ oder „Redner noch offen“. */
internal fun slotSpeakerLine(slot: TalkSlot): String {
    val external = slot.externalSpeaker
    val member = slot.speaker
    return when {
        external != null -> "${personName(slot)} · Gast aus ${external.congregationName}"
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
