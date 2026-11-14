package de.geosphere.congregationplaner.event.presentation

import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.event.domain.model.SpecialEvent
import de.geosphere.congregationplaner.presentation.components.ListDetailState
import de.geosphere.congregationplaner.talk.presentation.common.toDisplayText
import kotlinx.datetime.LocalDate

/** Das gerade bearbeitete Ereignis; [original] ist `null`, solange ein neues entsteht. */
data class SpecialEventEditorState(val event: SpecialEvent, val original: SpecialEvent? = null) {
    val isNew: Boolean get() = original == null
    val isDirty: Boolean get() = isNew || event != original

    /** Ein Beginn nach dem Ende verschiebt das Ende mit, so bleibt ein eintägiges Ereignis eintägig. */
    fun withStart(date: LocalDate): SpecialEventEditorState =
        copy(event = event.copy(startDate = date, endDate = maxOf(date, event.endDate)))

    /** Ein Ende vor dem Beginn verschiebt den Beginn mit. */
    fun withEnd(date: LocalDate): SpecialEventEditorState =
        copy(event = event.copy(startDate = minOf(date, event.startDate), endDate = date))

    /** „Alle“ schließt die übrigen Gruppen aus; wer eine Gruppe wählt, wählt „Alle“ ab. */
    fun toggled(group: EventAudience): SpecialEventEditorState {
        val audience = when {
            group == EventAudience.ALL -> setOf(EventAudience.ALL)
            group in event.audience -> event.audience - group
            else -> event.audience - EventAudience.ALL + group
        }
        return copy(event = event.copy(audience = audience))
    }
}

data class SpecialEventUiState(
    val events: List<SpecialEvent> = emptyList(),
    val isLoading: Boolean = true,
    val canManage: Boolean = false,
    val editor: SpecialEventEditorState? = null,
    override val errorMessage: String? = null,
    override val isSaving: Boolean = false,
) : ListDetailState {
    override val hasDetail: Boolean get() = editor != null
}

/** „Mo, 11.06.2027“ für einen Tag, sonst „Fr, 11.06.2027 – So, 13.06.2027“. */
fun SpecialEvent.dateRangeText(): String =
    if (isSingleDay) startDate.toDisplayText() else "${startDate.toDisplayText()} – ${endDate.toDisplayText()}"

/** Die Gruppen in fester Reihenfolge, z. B. „Brüder, Schwestern“. */
fun SpecialEvent.audienceText(): String = audience.sortedBy { it.ordinal }.joinToString(", ") { it.label }
