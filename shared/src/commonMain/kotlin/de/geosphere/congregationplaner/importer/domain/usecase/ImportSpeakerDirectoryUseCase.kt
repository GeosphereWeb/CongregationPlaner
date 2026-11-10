package de.geosphere.congregationplaner.importer.domain.usecase

import de.geosphere.congregationplaner.importer.domain.model.ImportReport
import de.geosphere.congregationplaner.importer.domain.planning.SpeakerDirectoryImport
import de.geosphere.congregationplaner.talk.domain.usecase.SaveExternalCircuitUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveExternalCongregationUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveVisitingSpeakerUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

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
