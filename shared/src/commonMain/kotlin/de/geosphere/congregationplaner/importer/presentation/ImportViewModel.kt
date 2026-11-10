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

data class ImportUiState(
    /** The sources this user may import; empty if the platform cannot import or the user has no right to. */
    val allowedSources: List<ImportSource> = emptyList(),
    val pending: PendingImport? = null,
    val report: ImportReport? = null,
    val reportSource: ImportSource? = null,
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
)

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
                pending = state.pending?.takeIf { it.source in allowed },
            )
        }
    }

    private fun canImport(user: AppUser, source: ImportSource) =
        if (source.needsGlobalAdmin) referencePolicy.canManage(user) else schedulePolicy.canPlanTalks(user)

    fun choose(source: ImportSource) {
        val user = currentUser
        if (user == null || mutableUiState.value.isBusy || source !in mutableUiState.value.allowedSources) return
        mutableUiState.update { it.copy(isBusy = true, errorMessage = null, report = null, pending = null) }
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
                    mutableUiState.update { it.copy(isBusy = false, pending = PendingImport(source, prepared)) }
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
                it.copy(isBusy = false, pending = null, report = report, reportSource = pending.source)
            }
        }
    }

    fun cancel() = mutableUiState.update { it.copy(pending = null) }

    fun dismissReport() = mutableUiState.update { it.copy(report = null, reportSource = null) }

    fun dismissError() = mutableUiState.update { it.copy(errorMessage = null) }

    private fun fail(message: String) = mutableUiState.update { it.copy(isBusy = false, errorMessage = message) }
}
