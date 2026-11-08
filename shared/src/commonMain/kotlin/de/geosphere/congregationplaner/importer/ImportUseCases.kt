package de.geosphere.congregationplaner.importer

import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectoryException
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.model.TalkReferenceException
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.usecase.SaveExternalCircuitUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveExternalCongregationUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveTalkHistoryEntryUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveTalkSlotUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveTalkUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveVisitingSpeakerUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveWeekEventUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/** The result of writing a plan: what was written, what was left alone and what failed (with the reason). */
data class ImportReport(
    val created: Int = 0,
    val updated: Int = 0,
    val skipped: Int = 0,
    val failures: List<String> = emptyList(),
) {
    operator fun plus(other: ImportReport) = ImportReport(
        created + other.created,
        updated + other.updated,
        skipped + other.skipped,
        failures + other.failures,
    )
}

private class Written<T>(val report: ImportReport, val saved: List<PlanEntry<T>>)

private fun Throwable.isDenied(): Boolean = this is TalkReferenceException.NotAllowed ||
    this is ExternalDirectoryException.NotAllowed ||
    this is TalkScheduleException.NotAllowed

/**
 * Writes the entries of [plan] that are to be created or updated through the regular save use case, so permission,
 * validation and rules apply as in the app. A failing entry is reported and the run continues; a missing permission
 * stops the step at once, because every further entry would fail the same way.
 */
private suspend fun <T> write(plan: ImportPlan<T>, label: (T) -> String, save: suspend (T) -> Result<*>): Written<T> {
    var report = ImportReport(skipped = plan.count(ImportKind.SKIP))
    val saved = mutableListOf<PlanEntry<T>>()
    for (entry in plan.entries.filter { it.kind != ImportKind.SKIP }) {
        val failure = save(entry.item).exceptionOrNull()
        when {
            failure == null -> {
                saved += entry
                report += if (entry.kind == ImportKind.CREATE) ImportReport(created = 1) else ImportReport(updated = 1)
            }
            failure.isDenied() -> return Written(report + failed(failure.message, label(entry.item)), saved)
            else -> report += failed(failure.message, label(entry.item))
        }
    }
    return Written(report, saved)
}

private fun failed(reason: String?, subject: String) = ImportReport(failures = listOf("$subject: $reason"))

/** Writes week events and the talk catalog; both need a global admin. */
@Inject
class ImportReferenceDataUseCase(private val saveEvent: SaveWeekEventUseCase, private val saveTalk: SaveTalkUseCase) {
    suspend operator fun invoke(user: AppUser, events: ImportPlan<WeekEvent>, talks: ImportPlan<Talk>): ImportReport {
        val eventReport = write(events, { "Wochenereignis ${it.name}" }) { saveEvent(user, it) }.report
        val talkReport = write(talks, { "Vortrag ${it.number}" }) { saveTalk(user, it) }.report
        return eventReport + talkReport
    }
}

/** Writes circuits, congregations and speakers in this order (each needs the previous one); needs a talk planner. */
@Inject
class ImportSpeakerDirectoryUseCase(
    private val saveCircuit: SaveExternalCircuitUseCase,
    private val saveCongregation: SaveExternalCongregationUseCase,
    private val saveSpeaker: SaveVisitingSpeakerUseCase,
) {
    suspend operator fun invoke(user: AppUser, import: SpeakerDirectoryImport): ImportReport {
        val circuits = write(import.circuits, { "Kreis ${it.name}" }) { saveCircuit(user, it) }.report
        val congregations =
            write(import.congregations, { "Versammlung ${it.name}" }) { saveCongregation(user, it) }.report
        val speakers = write(import.speakers, { "Redner ${it.lastName}" }) { saveSpeaker(user, it) }.report
        return circuits + congregations + speakers
    }
}

/**
 * Writes the speakers that only occur in the schedule, the slots, and then the history of exactly the slots that were
 * written ([existingHistory] is merged in), so a failed slot leaves no history behind.
 */
@Inject
class ImportTalkScheduleUseCase(
    private val saveSpeaker: SaveVisitingSpeakerUseCase,
    private val saveSlot: SaveTalkSlotUseCase,
    private val saveHistory: SaveTalkHistoryEntryUseCase,
) {
    suspend operator fun invoke(
        user: AppUser,
        import: TalkScheduleImport,
        existingHistory: List<TalkHistoryEntry>,
    ): ImportReport {
        val newSpeakers = ImportPlan(import.newSpeakers.map { PlanEntry(it, ImportKind.CREATE) }, emptyList())
        val speakers = write(newSpeakers, { "Redner ${it.lastName}" }) { saveSpeaker(user, it) }.report
        val slots = write(import.slots, { "Termin ${it.date}" }) { saveSlot(user, it) }
        val written = ImportPlan<TalkSlot>(slots.saved, emptyList())
        val history = write(planTalkHistory(written, existingHistory), { "Historie Vortrag ${it.talkNumber}" }) {
            saveHistory(user, it)
        }.report
        return speakers + slots.report + history
    }
}
