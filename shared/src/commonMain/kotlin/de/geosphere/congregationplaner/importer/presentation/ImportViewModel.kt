package de.geosphere.congregationplaner.importer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.importer.data.ImportFileSource
import de.geosphere.congregationplaner.importer.domain.export.ExportKind
import de.geosphere.congregationplaner.importer.domain.export.ExportTableUseCase
import de.geosphere.congregationplaner.importer.domain.model.ImportReport
import de.geosphere.congregationplaner.importer.domain.usecase.ImportSource
import de.geosphere.congregationplaner.importer.domain.usecase.MissingImportCountryException
import de.geosphere.congregationplaner.importer.domain.usecase.PrepareImportUseCase
import de.geosphere.congregationplaner.importer.domain.usecase.PreparedImport
import de.geosphere.congregationplaner.organization.domain.usecase.ListCountriesUseCase
import de.geosphere.congregationplaner.organization.presentation.country.CountryChoiceHolder
import de.geosphere.congregationplaner.organization.presentation.country.CountryChoiceState
import de.geosphere.congregationplaner.talk.domain.permission.TalkReferenceDataPolicy
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Where the import is shown: the settings keep the imports of the global admin (reference data of a country), the talk
 * planning tab has the imports and exports of the talk planner.
 */
enum class ImportArea { SETTINGS, TALK_PLANNING }

/** One export button: the schedule once per offered year, the lists once ([year] then only names the request). */
data class ExportOption(val kind: ExportKind, val year: Int, val label: String)

/** A parsed file waiting for the user's confirmation. */
class PendingImport(val source: ImportSource, val prepared: PreparedImport)

/** Where the import stands; a preview and a report never show at the same time. */
sealed interface ImportStep {
    data object Idle : ImportStep

    /** A parsed file waits for the user's confirmation. */
    data class Preview(val pending: PendingImport) : ImportStep

    /** The import ran; [report] says what was written. */
    data class Done(val source: ImportSource, val report: ImportReport) : ImportStep
}

data class ImportUiState(
    /** The sources this user may import; empty if the platform cannot import or the user has no right to. */
    val allowedSources: List<ImportSource> = emptyList(),
    /** The target country of week events and talks; only loaded for users who may import them. */
    val country: CountryChoiceState = CountryChoiceState(),
    val step: ImportStep = ImportStep.Idle,
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    /** What this user may export here; empty outside the talk planning or without a file dialog. */
    val exportKinds: List<ExportKind> = emptyList(),
    /** The years offered for the schedule export: the current and the next one. */
    val exportYears: List<Int> = emptyList(),
    /** The file name of the last saved export, to confirm it. */
    val savedFile: String? = null,
) {
    val pending: PendingImport? get() = (step as? ImportStep.Preview)?.pending
    val report: ImportReport? get() = (step as? ImportStep.Done)?.report
    val reportSource: ImportSource? get() = (step as? ImportStep.Done)?.source

    val exportOptions: List<ExportOption>
        get() = exportKinds.flatMap { kind ->
            if (kind == ExportKind.SCHEDULE) {
                exportYears.map { ExportOption(kind, it, "${kind.label} $it") }
            } else {
                exportYears.take(1).map { ExportOption(kind, it, kind.label) }
            }
        }

    /** Whether the country picker belongs on screen: some allowed source goes into a country. */
    val needsCountry: Boolean get() = allowedSources.any { it.needsGlobalAdmin }
}

/** Drives the import: choose a file, preview what will happen, confirm, show the report. */
@Inject
class ImportViewModel(
    private val fileSource: ImportFileSource,
    private val prepare: PrepareImportUseCase,
    private val referencePolicy: TalkReferenceDataPolicy,
    private val schedulePolicy: TalkSchedulePermissionPolicy,
    listCountries: ListCountriesUseCase,
    private val exportTable: ExportTableUseCase,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ImportUiState())
    val uiState = mutableUiState.asStateFlow()

    val countries = CountryChoiceHolder(
        viewModelScope,
        listCountries,
        read = { mutableUiState.value.country },
        write = { country -> mutableUiState.update { it.copy(country = country) } },
    )

    private var currentUser: AppUser? = null

    fun onCurrentUserChanged(
        user: AppUser,
        area: ImportArea = ImportArea.SETTINGS,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    ) {
        currentUser = user
        val available = fileSource.isAvailable
        val allowed = if (available) ImportSource.entries.filter { canImport(user, it, area) } else emptyList()
        val exports = available && area == ImportArea.TALK_PLANNING && schedulePolicy.canPlanTalks(user)
        mutableUiState.update { state ->
            state.copy(
                allowedSources = allowed,
                exportKinds = if (exports) ExportKind.entries else emptyList(),
                exportYears = listOf(today.year, today.year + 1),
                step = state.step.takeUnless { it is ImportStep.Preview && it.pending.source !in allowed }
                    ?: ImportStep.Idle,
            )
        }
        if (mutableUiState.value.needsCountry) countries.load(user.congregation)
    }

    private fun canImport(user: AppUser, source: ImportSource, area: ImportArea) = when (area) {
        ImportArea.SETTINGS -> source.needsGlobalAdmin && referencePolicy.canManage(user)
        ImportArea.TALK_PLANNING -> !source.needsGlobalAdmin && schedulePolicy.canPlanTalks(user)
    }

    /** Saves [kind] (for the schedule: the slots of [year]) as an `.xlsx` file the user picks. */
    fun export(kind: ExportKind, year: Int) {
        val user = currentUser
        if (user == null || mutableUiState.value.isBusy || kind !in mutableUiState.value.exportKinds) return
        mutableUiState.update { it.copy(isBusy = true, errorMessage = null, savedFile = null) }
        viewModelScope.launch {
            val file = exportTable(user, kind, year).getOrElse {
                return@launch fail("Der Export konnte nicht erstellt werden: ${it.message}")
            }
            fileSource.saveTable(file.fileName, file.table, file.dateHeaders)
                .onSuccess { saved ->
                    mutableUiState.update {
                        it.copy(isBusy = false, savedFile = file.fileName.takeIf { saved })
                    }
                }
                .onFailure { fail("Die Datei konnte nicht gespeichert werden: ${it.message}") }
        }
    }

    fun dismissSaved() = mutableUiState.update { it.copy(savedFile = null) }

    fun choose(source: ImportSource) {
        val user = currentUser
        if (user == null || mutableUiState.value.isBusy || source !in mutableUiState.value.allowedSources) return
        val country = mutableUiState.value.country.selected
        if (source.needsGlobalAdmin && country == null) return fail(MissingImportCountryException().message.orEmpty())
        mutableUiState.update { it.copy(isBusy = true, errorMessage = null, step = ImportStep.Idle) }
        viewModelScope.launch {
            val table = fileSource.pickTable("${source.label}: ${source.fileHint}").getOrElse {
                return@launch fail("Die Datei konnte nicht gelesen werden: ${it.message}")
            }
            if (table == null) {
                mutableUiState.update { it.copy(isBusy = false) }
                return@launch
            }
            prepare(user, source, table, country)
                .onSuccess { prepared ->
                    mutableUiState.update {
                        it.copy(isBusy = false, step = ImportStep.Preview(PendingImport(source, prepared)))
                    }
                }
                .onFailure { fail("Die Vorschau konnte nicht erstellt werden: ${it.message}") }
        }
    }

    fun confirm() {
        val pending = mutableUiState.value.pending
        if (pending == null || mutableUiState.value.isBusy) return
        mutableUiState.update { it.copy(isBusy = true, errorMessage = null) }
        viewModelScope.launch {
            val report = pending.prepared.run()
            mutableUiState.update {
                it.copy(isBusy = false, step = ImportStep.Done(pending.source, report))
            }
        }
    }

    fun cancel() = mutableUiState.update { if (it.step is ImportStep.Preview) it.copy(step = ImportStep.Idle) else it }

    fun dismissReport() = mutableUiState.update {
        if (it.step is ImportStep.Done) it.copy(step = ImportStep.Idle) else it
    }

    fun dismissError() = mutableUiState.update { it.copy(errorMessage = null) }

    private fun fail(message: String) = mutableUiState.update { it.copy(isBusy = false, errorMessage = message) }
}
