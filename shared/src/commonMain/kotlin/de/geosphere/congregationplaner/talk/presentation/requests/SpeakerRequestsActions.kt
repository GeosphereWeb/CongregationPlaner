package de.geosphere.congregationplaner.talk.presentation.requests

import de.geosphere.congregationplaner.talk.domain.model.ContactKind
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.MemberRef
import de.geosphere.congregationplaner.talk.domain.model.OpenRequest
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot

/** Callbacks der Übersicht; gebündelt, damit die Composables überschaubare Signaturen behalten. */
internal data class SpeakerRequestsActions(
    val onNew: () -> Unit = {},
    val onContact: (OpenRequest) -> Unit = {},
    val onSaveContact: (ContactKind, String) -> Unit = { _, _ -> },
    val onAccept: (OpenRequest) -> Unit = {},
    val onAskDecline: (OpenRequest) -> Unit = {},
    val onConfirmDecline: () -> Unit = {},
    val onAssign: (OpenRequest) -> Unit = {},
    val onPickDay: (TalkSlot) -> Unit = {},
    val onSelectSpeaker: (MemberRef?) -> Unit = {},
    val onSelectExternal: (ExternalSpeaker?) -> Unit = {},
    val onFreeSpeaker: (String) -> Unit = {},
    val onSelectTalk: (Talk?) -> Unit = {},
    val onFreeTalk: (String) -> Unit = {},
    val onNoteChange: (String) -> Unit = {},
    val onSaveNew: () -> Unit = {},
    val onDismissDialog: () -> Unit = {},
    val onDismissError: () -> Unit = {},
)
