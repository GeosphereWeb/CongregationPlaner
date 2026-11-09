package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.datetime.LocalDate

/**
 * Position in the schedule list of the first slot on or after [from], or null if there is none.
 * The list shows one month header before the first slot of each month, so every month up to and including
 * the target's own adds one item.
 */
internal fun slotListIndex(slots: List<TalkSlot>, from: LocalDate): Int? {
    val target = slots.indexOfFirst { it.date >= from }
    if (target < 0) return null
    val monthHeaders = slots.take(target + 1).map { it.date.month }.distinct().size
    return target + monthHeaders
}
