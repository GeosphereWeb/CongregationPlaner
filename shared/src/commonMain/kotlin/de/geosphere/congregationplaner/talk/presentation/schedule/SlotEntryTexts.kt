package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot

/** Der gewählte Vortrag als Text; ein frei eingetragener Vortrag nur mit Titel, `null` ohne Vortrag. */
internal fun slotTalkText(slot: TalkSlot): String? {
    val title = slot.talkTitle?.takeIf { it.isNotBlank() }
    val number = slot.talkNumber
    return when {
        number != null -> title?.let { "Nr. $number – $it" } ?: "Nr. $number"
        else -> title
    }
}

/** Der gewählte Redner als Text; `null` ohne Redner. */
internal fun slotSpeakerText(slot: TalkSlot): String? = slot.speakerDisplayName
