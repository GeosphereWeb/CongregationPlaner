package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.TalkPlanningSettings
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * Ändert die Stichtage des Planungsassistenten der Versammlung und speichert sie sofort; `null` hebt die Sperre auf.
 * Die Anzeige folgt sofort, die gespeicherten Werte kommen über die Beobachtung zurück.
 */
class PlanningSettingsHolder(
    private val session: TalkScheduleSession,
    private val useCases: ScheduleReferenceUseCases,
) {
    private val store get() = session.store

    fun setSpeakerFreeSince(date: LocalDate?) = change { it.copy(speakerFreeSince = date) }

    fun setTalkFreeSince(date: LocalDate?) = change { it.copy(talkFreeSince = date) }

    private fun change(update: (TalkPlanningSettings) -> TalkPlanningSettings) {
        val user = session.allowedUser() ?: return
        val settings = update(store.value.planningSettings)
        store.update { it.copy(planningSettings = settings, errorMessage = null) }
        session.scope.launch {
            useCases.savePlanningSettings(user, settings)
                .onFailure { session.fail(it, "Die Stichtage konnten nicht gespeichert werden.") }
        }
    }
}
