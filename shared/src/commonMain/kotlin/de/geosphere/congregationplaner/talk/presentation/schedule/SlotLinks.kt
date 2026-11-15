package de.geosphere.congregationplaner.talk.presentation.schedule

/** Hinweis an der Vortragskarte, wenn sie zur Vortragshistorie führt. */
internal const val TALK_LINK_HINT = "Antippen: zur Vortragshistorie"

/** Hinweis an der Rednerkarte, wenn sie zur Rednerliste führt. */
internal const val SPEAKER_LINK_HINT = "Antippen: zur Rednerliste"

/**
 * Der Vortrag, zu dem der Editor in die Vortragshistorie springen kann; `null` ohne Vortragsnummer und solange
 * Änderungen ungespeichert sind oder gespeichert werden, denn beim Wechsel gingen sie verloren.
 */
internal fun TalkSlotEditorState.historyTalkNumber(isSaving: Boolean): Int? =
    slot.talkNumber.takeUnless { isDirty || isSaving }

/**
 * Der externe Redner, dessen Eintrag in der Rednerliste der Editor öffnen kann; eigene Mitglieder und frei
 * eingetragene Redner stehen nicht in der Rednerliste. `null` auch, solange Änderungen ungespeichert sind.
 */
internal fun TalkSlotEditorState.directorySpeakerId(isSaving: Boolean): String? =
    slot.externalSpeakerId.takeUnless { isDirty || isSaving }

/** Der Zusatz einer Feldkarte samt Hinweis auf den Sprung; `null`, wenn beides fehlt. */
internal fun withLinkHint(supporting: String?, linkHint: String?): String? =
    listOfNotNull(supporting, linkHint).joinToString("\n").ifEmpty { null }
