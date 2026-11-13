package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.talkLabel

/** Der gewählte Vortrag als Text; ein frei eingetragener Vortrag nur mit Titel, `null` ohne Vortrag. */
internal fun slotTalkText(slot: TalkSlot): String? = slot.talkLabel()

/** Der gewählte Redner als Text; `null` ohne Redner. */
internal fun slotSpeakerText(slot: TalkSlot): String? = slot.speakerDisplayName
