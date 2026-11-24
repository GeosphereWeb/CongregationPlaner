package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.sameSpeakerAs

/**
 * Platzhalter im Editor für eine Anfrage, die es noch nicht gibt; die echte ID vergibt das Speichern
 * (SaveEditedSlotUseCase), das den Wert immer ersetzt.
 */
internal const val NEW_REQUEST_ID = "new"

/** Eine Anfrage braucht einen Redner und einen Vortrag, der nicht entfällt. */
internal val TalkSlot.canBeRequested: Boolean get() = hasSpeaker && !talkCancelled

/** Der Termin mit eingeschaltetem oder ausgeschaltetem Schalter „Angefragt“; angefragt ist er nie „Informiert“. */
internal fun TalkSlot.withRequested(requested: Boolean, original: TalkSlot?): TalkSlot =
    if (requested) {
        copy(requestId = original?.requestId ?: NEW_REQUEST_ID, confirmed = false)
    } else {
        copy(requestId = null)
    }

/** Rückfrage, wenn Speichern eine offene Anfrage löscht, weil der Redner ersetzt oder entfernt wurde. */
internal fun requestReplacementWarning(original: TalkSlot?, edited: TalkSlot): String? =
    original?.takeIf { it.isRequested && !it.sameSpeakerAs(edited) }?.let {
        "Die offene Anfrage bei ${it.speakerDisplayName ?: "dem Redner"} wird gelöscht. Trotzdem speichern?"
    }
