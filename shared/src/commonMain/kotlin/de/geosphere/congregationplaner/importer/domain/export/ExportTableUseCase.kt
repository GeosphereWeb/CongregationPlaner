package de.geosphere.congregationplaner.importer.domain.export

import de.geosphere.congregationplaner.importer.domain.model.ImportTable
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveExternalDirectoryUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/** What the talk planner can export; the file names match the import hints, so a file goes back in unchanged. */
enum class ExportKind(val label: String) {
    SCHEDULE("Wochenplan"),
    SPEAKERS("Rednerliste"),
    TALK_CATALOG("Vortragsliste"),
}

/** A table ready to save: [dateHeaders] are the columns holding Excel serial dates. */
class ExportFile(val fileName: String, val table: ImportTable, val dateHeaders: Set<String> = emptySet())

/**
 * Reads the current data of the own congregation (slots of [year], speaker list, talk list of its country) and turns it
 * into an [ExportFile] in the columns of the import. Only talk planners may export.
 */
@Inject
class ExportTableUseCase(
    private val policy: TalkSchedulePermissionPolicy,
    private val schedule: TalkScheduleRepository,
    private val directory: ObserveExternalDirectoryUseCase,
    private val catalog: TalkCatalogRepository,
) {
    suspend operator fun invoke(user: AppUser, kind: ExportKind, year: Int): Result<ExportFile> = try {
        val congregation = user.congregation
        if (!policy.canPlanTalks(user) || congregation == null) throw TalkScheduleException.NotAllowed()
        Result.success(
            when (kind) {
                ExportKind.SCHEDULE -> ExportFile(
                    "Export_Vortragsplanung_$year.xlsx",
                    exportTalkSchedule(schedule.observeYear(congregation, year).first()),
                    setOf(SCHEDULE_DATE),
                )
                ExportKind.SPEAKERS -> ExportFile(
                    "Export_Kreis-Versl-Redner-Vortraege.xlsx",
                    exportSpeakerDirectory(directory(user).first()),
                )
                ExportKind.TALK_CATALOG -> ExportFile(
                    "Export_tblVortaege.xlsx",
                    exportTalkCatalog(catalog.observeTalks(congregation.country).first()),
                )
            },
        )
    } catch (exception: CancellationException) {
        throw exception
    } catch (@Suppress("TooGenericExceptionCaught") exception: Exception) {
        Result.failure(exception)
    }
}
