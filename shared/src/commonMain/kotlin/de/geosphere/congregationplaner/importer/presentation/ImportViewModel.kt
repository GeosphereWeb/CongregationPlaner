package de.geosphere.congregationplaner.importer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.importer.data.ImportFileSource
import de.geosphere.congregationplaner.importer.domain.model.ImportReport
import de.geosphere.congregationplaner.importer.domain.usecase.ImportSource
import de.geosphere.congregationplaner.importer.domain.usecase.PrepareImportUseCase
import de.geosphere.congregationplaner.importer.domain.usecase.PreparedImport
import de.geosphere.congregationplaner.talk.domain.permission.TalkReferenceDataPolicy
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val step: ImportStep = ImportStep.Idle,
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
) {
    val pending: PendingImport? get() = (step as? ImportStep.Preview)?.pending
    val report: ImportReport? get() = (step as? ImportStep.Done)?.report
    val reportSource: ImportSource? get() = (step as? ImportStep.Done)?.source
}

/** Drives the import: choose a file, preview what will happen, confirm, show the report. */
@Inject
class ImportViewModel(
    private val fileSource: ImportFileSource,
    private val prepare: PrepareImportUseCase,
    private val referencePolicy: TalkReferenceDataPolicy,
    private val schedulePolicy: TalkSchedulePermissionPolicy,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ImportUiState())
    val uiState = mutableUiState.asStateFlow()

    private var currentUser: AppUser? = null

    fun onCurrentUserChanged(user: AppUser) {
        currentUser = user
        val allowed = if (fileSource.isAvailable) ImportSource.entries.filter { canImport(user, it) } else emptyList()
        mutableUiState.update { state ->
            state.copy(
                allowedSources = allowed,
                step = state.step.takeUnless { it is ImportStep.Preview && it.pending.source !in allowed }
                    ?: ImportStep.Idle,
            )
        }
    }

    private fun canImport(user: AppUser, source: ImportSource) =
        if (source.needsGlobalAdmin) referencePolicy.canManage(user) else schedulePolicy.canPlanTalks(user)

    fun choose(source: ImportSource) {
        val user = currentUser
        if (user == null || mutableUiState.value.isBusy || source !in mutableUiState.value.allowedSources) return
        mutableUiState.update { it.copy(isBusy = true, errorMessage = null, step = ImportStep.Idle) }
        viewModelScope.launch {
            val table = fileSource.pickTable("${source.label}: ${source.fileHint}").getOrElse {
                return@launch fail("Die Datei konnte nicht gelesen werden: ${it.message}")
            }
            if (table == null) {
                mutableUiState.update { it.copy(isBusy = false) }
                return@launch
            }
            prepare(user, source, table)
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
