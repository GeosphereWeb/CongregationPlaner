package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.presentation.components.CardHighlight
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.ui.chip.StatusTone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

/** Text und Farbe der Statusmarke eines Termins. */
internal data class SlotChip(val text: String, val tone: StatusTone)

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

/** Text und Farbe der Statusmarke; der Name des Wochenereignisses steht nie in der Marke, sondern in [slotEventLine]. */
internal fun slotChip(status: SlotStatus): SlotChip = when (status) {
    SlotStatus.NEXT -> SlotChip(statusLabel(status), StatusTone.Gold)
    SlotStatus.CANCELLED -> SlotChip(statusLabel(status), StatusTone.Sky)
    SlotStatus.NOT_PLANNED -> SlotChip(statusLabel(status), StatusTone.Coral)
    SlotStatus.PLANNED -> SlotChip(statusLabel(status), StatusTone.Sky)
    SlotStatus.INFORMED -> SlotChip(statusLabel(status), StatusTone.Mint)
}
