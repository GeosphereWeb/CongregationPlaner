package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot

/** Hinweis an frei eingetragenen Einträgen, die in keiner Liste stehen. */
internal const val FREE_ENTRY_HINT = "frei eingetragen"

/** Der gewählte Vortrag als Text; `null` ohne Vortrag. */
internal fun slotTalkText(slot: TalkSlot): String? {
    val title = slot.talkTitle?.takeIf { it.isNotBlank() }
    val number = slot.talkNumber
    return when {
        number != null -> title?.let { "Nr. $number – $it" } ?: "Nr. $number"
        title != null -> "$title ($FREE_ENTRY_HINT)"
        else -> null
    }
}

/** Der gewählte Redner als Text; ein frei eingetragener Redner mit Hinweis, `null` ohne Redner. */
internal fun slotSpeakerText(slot: TalkSlot): String? =
    slot.freeSpeakerName?.let { "$it ($FREE_ENTRY_HINT)" } ?: slot.speakerDisplayName
