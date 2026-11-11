package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.datetime.LocalTime

/** Die Termine je Monat, in der Reihenfolge der Liste. */
internal fun slotsByMonth(slots: List<TalkSlot>): List<List<TalkSlot>> = slots.groupBy { it.date.month }.values.toList()

/** Kürzt die Notiz auf die erlaubte Länge; eine leere Notiz entfällt. */
internal fun TalkSlot.withLimitedNote(): TalkSlot = copy(note = note?.take(TalkSlot.MAX_NOTE_LENGTH)?.ifEmpty { null })

/** Die häufigste Uhrzeit der [slots]; bei Gleichstand die früheste, ohne Termine `null`. */
internal fun commonSlotTime(slots: List<TalkSlot>): LocalTime? = slots.groupingBy { it.time }.eachCount().entries
    .sortedWith(compareBy({ -it.value }, { it.key }))
    .firstOrNull()?.key
