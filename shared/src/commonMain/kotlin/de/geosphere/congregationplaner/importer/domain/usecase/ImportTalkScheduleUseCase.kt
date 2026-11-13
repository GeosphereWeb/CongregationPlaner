package de.geosphere.congregationplaner.importer.domain.usecase

import de.geosphere.congregationplaner.importer.domain.model.ImportReport
import de.geosphere.congregationplaner.importer.domain.planning.TalkScheduleImport
import de.geosphere.congregationplaner.talk.domain.usecase.SaveTalkSlotUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/** Writes the slots; the talk history follows from them. */
@Inject
class ImportTalkScheduleUseCase(private val saveSlot: SaveTalkSlotUseCase) {
    suspend operator fun invoke(user: AppUser, import: TalkScheduleImport): ImportReport =
        write(import.slots, { "Termin ${it.date}" }) { saveSlot(user, it) }.report
}
