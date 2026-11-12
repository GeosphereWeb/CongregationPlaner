package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.upcomingTalkSlots
import kotlinx.datetime.LocalDate

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

/** Der erste Termin ab [today], an dem ein Vortrag gehalten wird; abgesagte Termine zählen nicht. */
internal fun nextSlotDate(slots: List<TalkSlot>, today: LocalDate): LocalDate? =
    slots.upcomingTalkSlots(today).firstOrNull()?.date

/**
 * Reihenfolge der Prüfung: abgesagt, nächster Termin, dann nach Vollständigkeit. „Nächster“ gilt damit nur für einen
 * Termin, an dem ein Vortrag stattfindet.
 */
internal fun slotStatus(slot: TalkSlot, nextSlotDate: LocalDate?): SlotStatus = when {
    slot.talkCancelled -> SlotStatus.CANCELLED
    slot.date == nextSlotDate -> SlotStatus.NEXT
    !slot.hasTalk || !slot.hasSpeaker -> SlotStatus.NOT_PLANNED
    slot.confirmed -> SlotStatus.INFORMED
    else -> SlotStatus.PLANNED
}
