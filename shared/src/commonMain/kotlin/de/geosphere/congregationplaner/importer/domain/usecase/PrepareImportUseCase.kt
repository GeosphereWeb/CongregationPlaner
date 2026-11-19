package de.geosphere.congregationplaner.importer.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.event.domain.repository.EventTypeRepository
import de.geosphere.congregationplaner.event.domain.usecase.SaveEventTypeUseCase
import de.geosphere.congregationplaner.importer.domain.model.ImportError
import de.geosphere.congregationplaner.importer.domain.model.ImportKind
import de.geosphere.congregationplaner.importer.domain.model.ImportPlan
import de.geosphere.congregationplaner.importer.domain.model.ImportReport
import de.geosphere.congregationplaner.importer.domain.model.ImportTable
import de.geosphere.congregationplaner.importer.domain.planning.ScheduleImportContext
import de.geosphere.congregationplaner.importer.domain.planning.eventTypeIdsBySource
import de.geosphere.congregationplaner.importer.domain.planning.excelSerialToDate
import de.geosphere.congregationplaner.importer.domain.planning.planScheduleEventTypes
import de.geosphere.congregationplaner.importer.domain.planning.planSpeakerDirectory
import de.geosphere.congregationplaner.importer.domain.planning.planTalkCatalog
import de.geosphere.congregationplaner.importer.domain.planning.planTalkSchedule
import de.geosphere.congregationplaner.importer.domain.planning.planWeekEvents
import de.geosphere.congregationplaner.organization.domain.model.CountryRef
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveExternalDirectoryUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalTime

/**
 * The four source files of the old planning tool. Week events and talks belong to a country (ADR-0034): a global admin
 * imports them into a chosen country, the other files go into the own congregation.
 */
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

/** The reference data import needs a target country. */
class MissingImportCountryException : IllegalStateException("Bitte wähle zuerst das Land, in das importiert wird.")

/**
 * Reads the current data of the target (week events, catalog, directory, slots, meeting times), builds the
 * [ImportPlan]s for a parsed file and returns them as a [PreparedImport] to confirm. [country] is the target of week
 * events and talks; speakers check against the lists of the own congregation's country, the schedule against its event
 * types (ADR-0036), creating the missing ones when it runs.
 */
@Suppress("LongParameterList")
@Inject
class PrepareImportUseCase(
    private val weekEvents: WeekEventRepository,
    private val eventTypes: EventTypeRepository,
    private val saveEventType: SaveEventTypeUseCase,
    private val catalog: TalkCatalogRepository,
    private val directory: ObserveExternalDirectoryUseCase,
    private val schedule: TalkScheduleRepository,
    private val congregations: CongregationMetadataRepository,
    private val importReference: ImportReferenceDataUseCase,
    private val importDirectory: ImportSpeakerDirectoryUseCase,
    private val importSchedule: ImportTalkScheduleUseCase,
) {
    suspend operator fun invoke(
        user: AppUser,
        source: ImportSource,
        table: ImportTable,
        country: CountryRef? = null,
    ): Result<PreparedImport> = try {
        Result.success(
            when (source) {
                ImportSource.WEEK_EVENTS -> prepareWeekEvents(
                    user,
                    table,
                    country ?: throw MissingImportCountryException(),
                )
                ImportSource.TALK_CATALOG ->
                    prepareTalkCatalog(user, table, country ?: throw MissingImportCountryException())
                ImportSource.SPEAKERS -> prepareSpeakers(user, table)
                ImportSource.SCHEDULE -> prepareSchedule(user, table)
            },
        )
    } catch (exception: CancellationException) {
        throw exception
    } catch (@Suppress("TooGenericExceptionCaught") exception: Exception) {
        Result.failure(exception)
    }

    private suspend fun prepareWeekEvents(user: AppUser, table: ImportTable, country: CountryRef): PreparedImport {
        val plan = planWeekEvents(table, weekEvents.observeWeekEvents(country).first()).plan
        return PreparedImport(listOf(plan.summary("Wochenereignisse")), plan.errors, emptyList()) {
            importReference(user, country, plan, ImportPlan(emptyList(), emptyList()))
        }
    }

    private suspend fun prepareTalkCatalog(user: AppUser, table: ImportTable, country: CountryRef): PreparedImport {
        val plan = planTalkCatalog(table, catalog.observeTalks(country).first())
        return PreparedImport(listOf(plan.summary("Vorträge")), plan.errors, emptyList()) {
            importReference(user, country, ImportPlan(emptyList(), emptyList()), plan)
        }
    }

    private suspend fun ownTalks(user: AppUser): List<Talk> =
        user.congregation?.let { catalog.observeTalks(it.country).first() }.orEmpty()

    private suspend fun ownEventTypes(user: AppUser): List<EventType> =
        user.congregation?.let { eventTypes.observeEventTypes(it).first() }.orEmpty()

    private suspend fun prepareSpeakers(user: AppUser, table: ImportTable): PreparedImport {
        val known = ownTalks(user).map { it.number }.toSet()
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
        val types = ownEventTypes(user)
        val missingTypes = planScheduleEventTypes(table, types)
        // The preview plans with placeholders for the missing types; the run plans again with their real IDs.
        val placeholders = missingTypes.entries.map { it.item.copy(id = "new:${it.item.name}") }
        val context = ScheduleImportContext(
            existingSlots = existingSlots(user, years),
            catalog = ownTalks(user),
            eventTypes = types + placeholders,
            eventTypeIdBySourceId = eventTypeIdsBySource(table, types + placeholders),
            directory = currentDirectory(user),
            timeFor = { year -> times.getValue(year) },
        )
        val import = planTalkSchedule(table, context)
        return PreparedImport(
            listOf(missingTypes.summary("Ereignisarten"), import.slots.summary("Termine")),
            import.slots.errors,
            import.warnings,
        ) {
            val created = write(missingTypes, { "Ereignisart ${it.name}" }) { saveEventType(user, it) }.report
            val allTypes = ownEventTypes(user)
            val final = context.copy(
                eventTypes = allTypes,
                eventTypeIdBySourceId = eventTypeIdsBySource(table, allTypes),
            )
            created + importSchedule(user, planTalkSchedule(table, final))
        }
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
