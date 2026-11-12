package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.Chairman
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.Speaker
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent

/** Übernimmt, was im Editor aus Listen gewählt wird: Ereignis, Vortrag, Redner und Vorsitz. */
class SlotPickerHolder(private val session: TalkScheduleSession, private val editor: SlotEditorHolder) {
    fun selectWeekEvent(event: WeekEvent?) = pick { it.withWeekEvent(event) }

    fun selectTalk(talk: Talk?) = pick { it.withTalk(talk) }

    fun selectFreeSpeaker(name: String) = pick { it.withFreeSpeaker(name) }

    fun selectSpeaker(speaker: Speaker?) = pick { it.withSpeaker(speaker) }

    fun selectExternalSpeaker(speaker: ExternalSpeaker?) = pick { it.withExternalSpeaker(speaker) }

    fun selectChairman(chairman: Chairman?) = pick { it.withChairman(chairman) }

    // `null` heißt: Ein Mitglied wäre an einem Tag Redner und Vorsitzender; die Dialoge bieten es gar nicht erst an.
    private fun pick(change: (TalkSlot) -> TalkSlot?) {
        val slot = session.store.value.editor?.slot ?: return
        val changed = change(slot)
        if (changed == null) {
            session.store.update { it.copy(errorMessage = TalkScheduleException.SpeakerIsChairman().message) }
        } else {
            editor.update(changed)
        }
    }
}
