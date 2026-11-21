package de.geosphere.congregationplaner.importer.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.importer.di.ImportGraph
import de.geosphere.congregationplaner.importer.domain.export.ExportKind
import de.geosphere.congregationplaner.importer.domain.usecase.ImportSource
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.createGraph

/** The "Import/Export" tab of the talk planning: exports as Excel and the imports of the talk planner. */
@Composable
fun TalkImportExportRoute(currentUser: AppUser) {
    val viewModel = remember { createGraph<ImportGraph>().importViewModel }
    LaunchedEffect(viewModel, currentUser) { viewModel.onCurrentUserChanged(currentUser, ImportArea.TALK_PLANNING) }
    val state by viewModel.uiState.collectAsState()
    TalkImportExportContent(
        state,
        ImportActions(
            onChoose = viewModel::choose,
            onExport = viewModel::export,
            onDismissSaved = viewModel::dismissSaved,
            onConfirm = viewModel::confirm,
            onCancel = viewModel::cancel,
            onDismissReport = viewModel::dismissReport,
            onDismissError = viewModel::dismissError,
        ),
    )
}

@Composable
internal fun TalkImportExportContent(state: ImportUiState, actions: ImportActions, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        val width = Modifier.widthIn(max = 480.dp)
        if (state.exportKinds.isEmpty() && state.allowedSources.isEmpty()) {
            Text(
                "Import und Export gibt es in der Android- und Desktop-App.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        if (state.exportKinds.isNotEmpty()) ExportSection(state, actions, width)
        if (state.allowedSources.isNotEmpty()) ImportContent(state, actions, width)
    }
}

@Composable
private fun ExportSection(state: ImportUiState, actions: ImportActions, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Export als Excel", style = MaterialTheme.typography.titleSmall)
        Text(
            "Die Datei hat dieselben Spalten wie der Import; du kannst sie bearbeiten und wieder importieren.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val enabled = !state.isBusy && state.pending == null
        state.exportOptions.forEach { option ->
            OutlinedButton(
                onClick = { actions.onExport(option.kind, option.year) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(option.label) }
        }
        state.savedFile?.let { file ->
            Text("Gespeichert: $file", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = actions.onDismissSaved) { Text("Schließen") }
        }
    }
}

@ThemePreviews
@Composable
private fun TalkImportExportPreview() = PreviewThemeWrapper {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        val planner = ImportUiState(
            allowedSources = listOf(ImportSource.SPEAKERS, ImportSource.SCHEDULE),
            exportKinds = ExportKind.entries,
            exportYears = listOf(2026, 2027),
        )
        TalkImportExportContent(planner, ImportActions())
        TalkImportExportContent(planner.copy(savedFile = "Export_Vortragsplanung_2026.xlsx"), ImportActions())
        TalkImportExportContent(ImportUiState(), ImportActions())
    }
}
