package de.geosphere.congregationplaner.importer.domain.usecase

import de.geosphere.congregationplaner.importer.domain.model.ImportKind
import de.geosphere.congregationplaner.importer.domain.model.ImportPlan
import de.geosphere.congregationplaner.importer.domain.model.ImportReport
import de.geosphere.congregationplaner.importer.domain.model.PlanEntry
import de.geosphere.congregationplaner.importer.domain.planning.TalkScheduleImport
import de.geosphere.congregationplaner.importer.domain.planning.planTalkHistory
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.usecase.SaveTalkHistoryEntryUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveTalkSlotUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveVisitingSpeakerUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

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
