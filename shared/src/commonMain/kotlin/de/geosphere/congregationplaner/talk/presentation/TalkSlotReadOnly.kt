package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot

/** Die Angaben eines Termins für den Helfer, der sie nur lesen darf; leere Angaben entfallen. */
internal fun slotReadOnlyLines(slot: TalkSlot): List<String> = listOfNotNull(
    slot.weekEventName?.let { "Ereignis: $it" },
    "Zeit: ${slot.time}",
    if (slot.talkCancelled) {
        "Der Vortrag entfällt."
    } else {
        slot.talkNumber?.let { "Vortrag: Nr. $it – ${slot.talkTitle.orEmpty()}" } ?: "Noch kein Vortrag gewählt"
    },
    "Redner: ${slotSpeakerLine(slot)}".takeUnless { slot.talkCancelled },
    slot.chairman?.let { "Vorsitz: ${it.displayName}" },
    slot.note?.takeIf(String::isNotBlank)?.let { "Notiz: $it" },
)
