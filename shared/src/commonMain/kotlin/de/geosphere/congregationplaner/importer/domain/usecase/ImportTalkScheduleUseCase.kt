package de.geosphere.congregationplaner.importer.domain.usecase

import de.geosphere.congregationplaner.importer.domain.model.ImportKind
import de.geosphere.congregationplaner.importer.domain.model.ImportPlan
import de.geosphere.congregationplaner.importer.domain.model.ImportReport
import de.geosphere.congregationplaner.importer.domain.model.PlanEntry
import de.geosphere.congregationplaner.importer.domain.planning.TalkScheduleImport
import de.geosphere.congregationplaner.talk.domain.usecase.SaveTalkSlotUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveVisitingSpeakerUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/**
 * Writes the speakers that only occur in the schedule and then the slots; the talk history follows from the slots.
 */
@Inject
class ImportTalkScheduleUseCase(
    private val saveSpeaker: SaveVisitingSpeakerUseCase,
    private val saveSlot: SaveTalkSlotUseCase,
) {
    suspend operator fun invoke(user: AppUser, import: TalkScheduleImport): ImportReport {
        val newSpeakers = ImportPlan(import.newSpeakers.map { PlanEntry(it, ImportKind.CREATE) }, emptyList())
        val speakers = write(newSpeakers, { "Redner ${it.lastName}" }) { saveSpeaker(user, it) }.report
        val slots = write(import.slots, { "Termin ${it.date}" }) { saveSlot(user, it) }
        return speakers + slots.report
    }
}
