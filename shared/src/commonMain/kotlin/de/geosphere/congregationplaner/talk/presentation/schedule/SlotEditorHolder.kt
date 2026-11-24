package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.datetime.LocalDate

/** Öffnet, ändert und schließt den Editor eines Termins. */
class SlotEditorHolder(private val session: TalkScheduleSession, private val today: LocalDate) {
    private val store get() = session.store

    /** Ein neuer Termin, im laufenden Jahr ab heute, sonst ab dem 1. Januar des gewählten Jahres. */
    fun newSlot() {
        if (store.value.isSaving || session.allowedUser() == null) return
        val year = store.value.year
        val date = today.takeIf { it.year == year } ?: LocalDate(year, 1, 1)
        val slot = TalkSlot(date = date, time = CongregationMetadata().weekendMeetingTime)
        store.update { it.copy(editor = TalkSlotEditorState(slot), errorMessage = null) }
    }

    fun edit(slot: TalkSlot) {
        if (store.value.isSaving || session.allowedUser(confirmOnly = true) == null) return
        store.update { it.copy(editor = TalkSlotEditorState(slot, slot), errorMessage = null) }
    }

    /**
     * Während des Speicherns bleibt der Editor unverändert, sonst würde das Ergebnis die Eingaben überschreiben. Ohne
     * Redner gibt es keine Anfrage mehr; der Schalter „Angefragt“ geht dann aus.
     */
    fun update(slot: TalkSlot) = store.update {
        val updated = slot.withLimitedNote().let { s -> if (s.hasSpeaker) s else s.withRequested(false, null) }
        if (it.isSaving) it else it.copy(editor = it.editor?.copy(slot = updated))
    }

    fun cancel() {
        if (!store.value.isSaving) store.update { it.copy(editor = null, errorMessage = null) }
    }

    /** Verwirft Änderungen, lässt aber den ausgewählten Termin offen; ein neuer, ungespeicherter wird geschlossen. */
    fun discard() {
        if (store.value.isSaving) return
        store.update { state ->
            val editor = state.editor?.takeUnless { it.isNew }?.let { it.copy(slot = it.original ?: it.slot) }
            state.copy(editor = editor, errorMessage = null)
        }
    }

    /** Schalter „Angefragt“: nur mit Redner und Vortrag, der nicht entfällt; schaltet „Informiert“ aus. */
    fun setRequested(requested: Boolean) = store.update { state ->
        val editor = state.editor?.takeUnless { state.isSaving } ?: return@update state
        if (requested && !editor.slot.canBeRequested) {
            state
        } else {
            state.copy(editor = editor.copy(slot = editor.slot.withRequested(requested, editor.original)))
        }
    }

    /** Der Helfer ändert im Editor nur „Informiert“; beim Planer geht das über [update]. */
    fun setSpeakerInformed(confirmed: Boolean) = store.update { state ->
        val editor = state.editor
        if (state.isSaving || editor == null || !editor.slot.canBeInformed) {
            state
        } else {
            state.copy(editor = editor.copy(slot = editor.slot.copy(confirmed = confirmed)))
        }
    }
}
