package de.geosphere.congregationplaner.importer.domain.usecase

import de.geosphere.congregationplaner.importer.domain.model.ImportPlan
import de.geosphere.congregationplaner.importer.domain.model.ImportReport
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.usecase.SaveTalkUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveWeekEventUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/** Writes week events and the talk catalog; both need a global admin. */
@Inject
class ImportReferenceDataUseCase(private val saveEvent: SaveWeekEventUseCase, private val saveTalk: SaveTalkUseCase) {
    suspend operator fun invoke(user: AppUser, events: ImportPlan<WeekEvent>, talks: ImportPlan<Talk>): ImportReport {
        val eventReport = write(events, { "Wochenereignis ${it.name}" }) { saveEvent(user, it) }.report
        val talkReport = write(talks, { "Vortrag ${it.number}" }) { saveTalk(user, it) }.report
        return eventReport + talkReport
    }
}
