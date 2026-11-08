package de.geosphere.congregationplaner.importer

import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkHistoryRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveExternalDirectoryUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalTime

/** The four source files of the old planning tool. */
enum class ImportSource(val label: String, val fileHint: String, val needsGlobalAdmin: Boolean) {
    WEEK_EVENTS("Wochenereignisse", "tblWochenereignis.xlsx", needsGlobalAdmin = true),
    TALK_CATALOG("Vortragsliste", "Export_tblVortaege.xlsx", needsGlobalAdmin = true),
    SPEAKERS("Rednerliste", "Export_Kreis-Versl-Redner-Vortraege.xlsx", needsGlobalAdmin = false),
    SCHEDULE("Vortragsplanung", "Export_Vortragsplanung.xlsx", needsGlobalAdmin = false),
}

/** One line of the preview: how many entries of a kind will be created, updated or left alone. */
data class PlanSummary(val label: String, val create: Int, val update: Int, val skip: Int) {
    val description: String get() = "$label: $create anlegen, $update ändern, $skip unverändert"
}

/** The preview of one import: what [run] will do, and which rows cannot be imported. Nothing is written before. */
class PreparedImport(
    val summaries: List<PlanSummary>,
    val errors: List<ImportError>,
    val warnings: List<ImportError>,
    val run: suspend () -> ImportReport,
)

/**
 * Reads the current data of the target (week events, catalog, directory, slots, history, meeting times), builds the
 * [ImportPlan]s for a parsed file and returns them as a [PreparedImport] to confirm.
 */
@Suppress("LongParameterList")
@Inject
class PrepareImportUseCase(
    private val weekEvents: WeekEventRepository,
    private val catalog: TalkCatalogRepository,
    private val directory: ObserveExternalDirectoryUseCase,
    private val schedule: TalkScheduleRepository,
    private val history: TalkHistoryRepository,
    private val congregations: CongregationMetadataRepository,
    private val importReference: ImportReferenceDataUseCase,
    private val importDirectory: ImportSpeakerDirectoryUseCase,
    private val importSchedule: ImportTalkScheduleUseCase,
) {
    suspend operator fun invoke(user: AppUser, source: ImportSource, table: ImportTable): Result<PreparedImport> = try {
        Result.success(
            when (source) {
                ImportSource.WEEK_EVENTS -> prepareWeekEvents(user, table)
                ImportSource.TALK_CATALOG -> prepareTalkCatalog(user, table)
                ImportSource.SPEAKERS -> prepareSpeakers(user, table)
                ImportSource.SCHEDULE -> prepareSchedule(user, table)
            },
        )
    } catch (exception: CancellationException) {
        throw exception
    } catch (@Suppress("TooGenericExceptionCaught") exception: Exception) {
        Result.failure(exception)
    }

    private suspend fun prepareWeekEvents(user: AppUser, table: ImportTable): PreparedImport {
        val plan = planWeekEvents(table, weekEvents.observeWeekEvents().first()).plan
        return PreparedImport(listOf(plan.summary("Wochenereignisse")), plan.errors, emptyList()) {
            importReference(user, plan, ImportPlan(emptyList(), emptyList()))
        }
    }

    private suspend fun prepareTalkCatalog(user: AppUser, table: ImportTable): PreparedImport {
        val plan = planTalkCatalog(table, catalog.observeTalks().first())
        return PreparedImport(listOf(plan.summary("Vorträge")), plan.errors, emptyList()) {
            importReference(user, ImportPlan(emptyList(), emptyList()), plan)
        }
    }

    private suspend fun prepareSpeakers(user: AppUser, table: ImportTable): PreparedImport {
        val known = catalog.observeTalks().first().map { it.number }.toSet()
        val plan = planSpeakerDirectory(table, currentDirectory(user), known)
        return PreparedImport(
            listOf(
                plan.circuits.summary("Kreise"),
                plan.congregations.summary("Versammlungen"),
                plan.speakers.summary("Redner"),
            ),
            plan.speakers.errors,
            emptyList(),
        ) { importDirectory(user, plan) }
    }

    private suspend fun prepareSchedule(user: AppUser, table: ImportTable): PreparedImport {
        val dates = table.rows.mapNotNull { row ->
            row.text("Datum")?.toDoubleOrNull()?.let { runCatching { excelSerialToDate(it) }.getOrNull() }
        }
        val years = dates.map { it.year }.distinct()
        val times = meetingTimes(user, years)
        val events = weekEvents.observeWeekEvents().first()
        val existingHistory = user.congregation?.let { history.observeAll(it).first() }.orEmpty()
        val context = ScheduleImportContext(
            existingSlots = existingSlots(user, years),
            catalog = catalog.observeTalks().first(),
            weekEvents = events,
            weekEventIdBySourceId = eventIdsByName(table, events.associate { it.name.lowercase() to it.id }),
            directory = currentDirectory(user),
            timeFor = { year -> times.getValue(year) },
        )
        val import = planTalkSchedule(table, context)
        val historyPlan = planTalkHistory(import.slots, existingHistory)
        return PreparedImport(
            listOf(import.slots.summary("Termine"), historyPlan.summary("Historie (nach dem Speichern der Termine)")),
            import.slots.errors,
            import.warnings + newSpeakerNotes(import),
        ) { importSchedule(user, import, existingHistory) }
    }

    private suspend fun currentDirectory(user: AppUser): ExternalDirectory = directory(user).first()

    private suspend fun existingSlots(user: AppUser, years: List<Int>) = user.congregation?.let { congregation ->
        years.flatMap { schedule.observeYear(congregation, it).first() }
    }.orEmpty()

    private suspend fun meetingTimes(user: AppUser, years: List<Int>): Map<Int, LocalTime> {
        val congregation = user.congregation ?: throw TalkScheduleException.NotAllowed()
        val own = congregations.observeAll(congregation.circuitIds).first()
            .firstOrNull { it.id == congregation.congregationId }
            ?: throw TalkScheduleException.CongregationNotFound()
        return years.associateWith { own.meetingTimesFor(it).weekendTime }
    }
}

private fun <T> ImportPlan<T>.summary(label: String) =
    PlanSummary(label, count(ImportKind.CREATE), count(ImportKind.UPDATE), count(ImportKind.SKIP))

private fun newSpeakerNotes(import: TalkScheduleImport): List<ImportError> = if (import.newSpeakers.isEmpty()) {
    emptyList()
} else {
    listOf(
        ImportError(0, "${import.newSpeakers.size} Redner nur in der Planung werden neu angelegt"),
    )
}

/** The schedule file carries event ID and name per row; match the name against the events that now exist. */
private fun eventIdsByName(table: ImportTable, idByName: Map<String, String>): Map<String, String> =
    table.rows.mapNotNull { row ->
        val sourceId = row.idText("ID_Wochenereignis") ?: return@mapNotNull null
        val target = row.text("Ereignis")?.let { idByName[it.lowercase()] } ?: return@mapNotNull null
        sourceId to target
    }.toMap()
