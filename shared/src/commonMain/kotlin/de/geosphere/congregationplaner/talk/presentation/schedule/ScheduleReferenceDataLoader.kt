package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveAllTalkSlotsUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveExternalSpeakersUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkCatalogUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkMembersUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkPlanningSettingsUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveWeekEventsUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveTalkPlanningSettingsUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Die UseCases für die Listen, aus denen der Editor wählt, und für die Stichtage des Planungsassistenten. */
@Suppress("LongParameterList") // ein reines Bündel der Abhängigkeiten, damit das ViewModel überschaubar bleibt
@Inject
class ScheduleReferenceUseCases(
    val observeWeekEvents: ObserveWeekEventsUseCase,
    val observeTalkCatalog: ObserveTalkCatalogUseCase,
    val observeTalkMembers: ObserveTalkMembersUseCase,
    val observeExternalSpeakers: ObserveExternalSpeakersUseCase,
    val observePlanningSettings: ObserveTalkPlanningSettingsUseCase,
    val savePlanningSettings: SaveTalkPlanningSettingsUseCase,
    val observeAllSlots: ObserveAllTalkSlotsUseCase,
    val talkPolicy: TalkSchedulePermissionPolicy,
    val userPolicy: UserPermissionPolicy,
)

/**
 * Beobachtet Wochenereignisse, Vortragsliste, Redner und externe Redner, solange der Nutzer sie braucht: Ereignisse
 * und Vortragsliste nur der Planer, die Mitglieder nur mit dem Recht, sie einzusehen.
 */
class ScheduleReferenceDataLoader(
    private val session: TalkScheduleSession,
    private val useCases: ScheduleReferenceUseCases,
) {
    private val jobs = mutableMapOf<Source, Job>()

    private enum class Source(val label: String) {
        WEEK_EVENTS("Wochenereignisse"),
        TALKS("Vortragsliste"),
        EXTERNAL_SPEAKERS("Externe Redner"),
        MEMBERS("Redner und Vorsitzende"),
        PLANNING_SETTINGS("Stichtage"),
        ALL_SLOTS("Termine für die Vorschläge"),
    }

    fun observe(user: AppUser) {
        val canPlan = useCases.talkPolicy.canPlanTalks(user)
        val canView = useCases.talkPolicy.canViewTalkPlanning(user)
        keep(Source.WEEK_EVENTS, canPlan, { useCases.observeWeekEvents() }) { state, events ->
            state.copy(weekEvents = events)
        }
        keep(Source.TALKS, canPlan, { useCases.observeTalkCatalog() }) { state, talks -> state.copy(talks = talks) }
        keep(Source.EXTERNAL_SPEAKERS, canView, { useCases.observeExternalSpeakers(user) }) { state, speakers ->
            state.copy(externalSpeakers = speakers)
        }
        keep(Source.PLANNING_SETTINGS, canView, { useCases.observePlanningSettings(user) }) { state, settings ->
            state.copy(planningSettings = settings)
        }
        // Nur Planer wählen Redner und Vorträge aus, also brauchen nur sie die Termine aller Jahre.
        keep(Source.ALL_SLOTS, canPlan, { useCases.observeAllSlots(user) }) { state, slots ->
            state.copy(allSlots = slots)
        }
        val canViewMembers = canView && useCases.userPolicy.canViewMembers(user)
        keep(Source.MEMBERS, canViewMembers, { useCases.observeTalkMembers(user) }) { state, members ->
            state.copy(speakers = members.speakers, chairmen = members.chairmen)
        }
    }

    private fun TalkScheduleUiState.withoutPersonsFrom(source: Source): TalkScheduleUiState = when (source) {
        Source.EXTERNAL_SPEAKERS -> copy(externalSpeakers = emptyList())
        Source.MEMBERS -> copy(speakers = emptyList(), chairmen = emptyList())
        Source.ALL_SLOTS -> copy(allSlots = emptyList())
        else -> this
    }

    // Läuft die Quelle schon, bleibt sie; ohne Recht endet sie. Ereignisse und Vortragsliste bleiben dann im Zustand,
    // weil sie global sind; Redner und externe Redner werden geleert.
    private fun <T> keep(
        source: Source,
        allowed: Boolean,
        flow: () -> Flow<T>,
        apply: (TalkScheduleUiState, T) -> TalkScheduleUiState,
    ) {
        if (!allowed) {
            jobs.remove(source)?.cancel()
            session.store.update { state -> state.withoutPersonsFrom(source) }
            return
        }
        if (source in jobs) return
        jobs[source] = session.scope.launch {
            flow()
                .catch { exception ->
                    // Der Flow ist beendet; ohne Entfernen würde er nie wieder gestartet.
                    jobs.remove(source)
                    val message = exception.message ?: "${source.label} konnten nicht geladen werden."
                    session.store.update { it.copy(errorMessage = message) }
                }.collect { value -> session.store.update { apply(it, value) } }
        }
    }
}
