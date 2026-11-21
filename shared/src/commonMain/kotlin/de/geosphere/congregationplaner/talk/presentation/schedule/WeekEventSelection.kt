package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot

internal const val NO_WEEK_EVENT = "Kein Ereignis"

/** Die aktuelle Auswahl im Wochenereignis-Menü; ein entferntes Ereignis behält seinen gespeicherten Namen. */
internal fun weekEventSelection(slot: TalkSlot): String =
    if (slot.weekEventId == null) NO_WEEK_EVENT else slot.weekEventName.orEmpty()
