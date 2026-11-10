package de.geosphere.congregationplaner.importer.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.importer.di.ImportGraph
import de.geosphere.congregationplaner.importer.domain.model.ImportError
import de.geosphere.congregationplaner.importer.domain.model.ImportReport
import de.geosphere.congregationplaner.importer.domain.usecase.ImportSource
import de.geosphere.congregationplaner.importer.domain.usecase.PlanSummary
import de.geosphere.congregationplaner.importer.domain.usecase.PreparedImport
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.createGraph

internal data class ImportActions(
    val onChoose: (ImportSource) -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onDismissReport: () -> Unit = {},
    val onDismissError: () -> Unit = {},
)

/** The import section of the settings; shows nothing where the user cannot import or the platform has no file dialog. */
@Composable
fun ImportSection(currentUser: AppUser, modifier: Modifier = Modifier) {
    val viewModel = remember { createGraph<ImportGraph>().importViewModel }
    LaunchedEffect(viewModel, currentUser) { viewModel.onCurrentUserChanged(currentUser) }
    val state by viewModel.uiState.collectAsState()
    if (state.allowedSources.isEmpty()) return
    ImportContent(
        state,
        ImportActions(
            onChoose = viewModel::choose,
            onConfirm = viewModel::confirm,
            onCancel = viewModel::cancel,
            onDismissReport = viewModel::dismissReport,
            onDismissError = viewModel::dismissError,
        ),
        modifier,
    )
}

@Composable
internal fun ImportContent(state: ImportUiState, actions: ImportActions, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Import aus dem alten Planungstool", style = MaterialTheme.typography.titleSmall)
        Text(
            "Wähle eine Excel-Datei. Vor dem Schreiben siehst du, was passiert.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.allowedSources.forEach { source ->
            OutlinedButton(
                onClick = { actions.onChoose(source) },
                enabled = !state.isBusy && state.pending == null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("${source.label} (${source.fileHint})") }
        }
        if (state.isBusy) CircularProgressIndicator()
        state.errorMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = actions.onDismissError) { Text("Schließen") }
        }
        state.pending?.let { PendingPreview(it, state.isBusy, actions) }
        state.report?.let { ReportView(state.reportSource, it, actions.onDismissReport) }
    }
}

@Composable
private fun PendingPreview(pending: PendingImport, isBusy: Boolean, actions: ImportActions) {
    val prepared = pending.prepared
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Vorschau: ${pending.source.label}", style = MaterialTheme.typography.titleSmall)
        prepared.summaries.forEach { Text(it.description, style = MaterialTheme.typography.bodyMedium) }
        Problems("Fehler (werden nicht importiert)", prepared.errors, MaterialTheme.colorScheme.error)
        Problems("Hinweise", prepared.warnings, MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = actions.onConfirm, enabled = !isBusy) { Text("Importieren") }
            TextButton(onClick = actions.onCancel, enabled = !isBusy) { Text("Abbrechen") }
        }
    }
}

@Composable
private fun Problems(title: String, problems: List<ImportError>, color: Color) {
    if (problems.isEmpty()) return
    Text("$title: ${problems.size}", style = MaterialTheme.typography.labelLarge, color = color)
    problems.shownProblems().forEach { Text(it.description, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun ReportView(source: ImportSource?, report: ImportReport, onDismiss: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Ergebnis: ${source?.label.orEmpty()}", style = MaterialTheme.typography.titleSmall)
        Text(report.description, style = MaterialTheme.typography.bodyMedium)
        report.failures.shownProblems().forEach {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        TextButton(onClick = onDismiss) { Text("Schließen") }
    }
}

private val previewPending = PendingImport(
    ImportSource.SCHEDULE,
    PreparedImport(
        summaries = listOf(PlanSummary("Termine", 199, 12, 50), PlanSummary("Historie", 80, 0, 0)),
        errors = listOf(ImportError(17, "Ungültiges Datum: abc")),
        warnings = listOf(ImportError(0, "6 Redner nur in der Planung werden neu angelegt")),
        run = { ImportReport() },
    ),
)

@ThemePreviews
@Composable
private fun ImportContentPreview() = PreviewThemeWrapper {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(16.dp)) {
        val sources = ImportSource.entries
        ImportContent(ImportUiState(allowedSources = sources), ImportActions())
        ImportContent(ImportUiState(allowedSources = sources, pending = previewPending), ImportActions())
        ImportContent(
            ImportUiState(
                allowedSources = sources,
                report = ImportReport(
                    created = 199,
                    updated = 12,
                    skipped = 50,
                    failures = listOf("Termin 2023-05-07: …"),
                ),
                reportSource = ImportSource.SCHEDULE,
                errorMessage = "Die Datei konnte nicht gelesen werden: keine gültige .xlsx-Datei",
                isBusy = true,
            ),
            ImportActions(),
        )
    }
}
