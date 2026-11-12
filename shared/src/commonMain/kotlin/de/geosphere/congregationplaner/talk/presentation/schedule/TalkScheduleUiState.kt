package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.presentation.components.ListDetailState
import de.geosphere.congregationplaner.talk.domain.model.Chairman
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.PlanningUsage
import de.geosphere.congregationplaner.talk.domain.model.Speaker
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkPlanningSettings
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.model.planningUsageOf
import kotlinx.datetime.LocalTime

/**
 * Der gerade bearbeitete Termin; [original] ist `null`, solange ein neuer Termin entsteht. [usage] sagt den
 * Auswahldialogen, wann Redner und Vorträge zuletzt eingeplant sind und welche der Stichtag sperrt.
 */
data class TalkSlotEditorState(
    val slot: TalkSlot,
    val original: TalkSlot? = null,
    val usage: PlanningUsage = PlanningUsage(),
) {
    val isNew: Boolean get() = original == null
    val isDirty: Boolean get() = slot != original
}

data class TalkScheduleUiState(
    val year: Int,
    val slots: List<TalkSlot> = emptyList(),
    /** Gewählte Stände; leer zeigt das ganze Jahr. Bleibt beim Jahreswechsel erhalten. */
    val statusFilter: Set<SlotStatus> = emptySet(),
    val weekEvents: List<WeekEvent> = emptyList(),
    val talks: List<Talk> = emptyList(),
    val speakers: List<Speaker> = emptyList(),
    val chairmen: List<Chairman> = emptyList(),
    val externalSpeakers: List<ExternalSpeaker> = emptyList(),
    /** Alle Termine der Versammlung über alle Jahre (nur für Planer), Grundlage der Vorschläge. */
    val allSlots: List<TalkSlot> = emptyList(),
    /** Die Stichtage des Planungsassistenten der Versammlung. */
    val planningSettings: TalkPlanningSettings = TalkPlanningSettings(),
    val isLoading: Boolean = true,
    val canView: Boolean = false,
    val canPlan: Boolean = false,
    /** Ohne diese Berechtigung können die Redner nicht geladen werden. */
    val canViewMembers: Boolean = false,
    val editor: TalkSlotEditorState? = null,
    /** `true`, solange die Rückfrage zu „Jahr anlegen“ offen ist. */
    val confirmCreateYear: Boolean = false,
    /** Die im Dialog „Uhrzeit ändern“ gewählte Uhrzeit; `null`, solange der Dialog zu ist. */
    val yearTimeDialog: LocalTime? = null,
    val infoMessage: String? = null,
    override val errorMessage: String? = null,
    override val isSaving: Boolean = false,
) : ListDetailState {
    override val hasDetail: Boolean get() = editor != null

    /** Der Editor samt Nutzung der Redner und Vorträge; der bearbeitete Termin selbst sperrt nichts. */
    val editorView: TalkSlotEditorState?
        get() = editor?.copy(usage = planningUsageOf(allSlots, planningSettings, editor.original?.date))
}
