package de.geosphere.congregationplaner.organization.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.model.SelectedCongregation
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews

/**
 * Auswahl der Versammlung im Login: entweder Ebene für Ebene (Region → … → Versammlung)
 * oder über einen Einladungslink des Versammlungsadmins.
 */
@Composable
@Suppress("LongParameterList", "DEPRECATION")
internal fun CongregationSelector(
    state: CongregationSelectionUiState,
    onStartPicking: () -> Unit,
    onCancelPicking: () -> Unit,
    onSelectUnit: (OrgLevel, String) -> Unit,
    onInviteLinkChange: (String) -> Unit,
    onApplyInviteLink: () -> Unit,
    onPasteInviteLink: (String?) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val selected = state.selected
        if (selected != null && !state.isPicking) {
            Text("Versammlung", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${selected.displayName} (Kreis ${selected.ref.circuitId})",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onStartPicking) { Text("Ändern") }
            }
            return@Column
        }

        Text("Versammlung auswählen", style = MaterialTheme.typography.labelLarge)
        state.levels.forEach { level ->
            OrgLevelDropdown(selection = level, onSelect = { unitId -> onSelectUnit(level.level, unitId) })
        }

        HorizontalDivider()
        Text("oder Einladungslink einfügen", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = state.inviteLinkInput,
            onValueChange = onInviteLinkChange,
            placeholder = { Text("${CongregationRef.INVITE_LINK_PREFIX}…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        val clipboardManager = LocalClipboardManager.current
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onApplyInviteLink,
                enabled = state.inviteLinkInput.isNotBlank() && !state.isResolvingLink,
            ) {
                Text(if (state.isResolvingLink) "Prüft …" else "Link übernehmen")
            }
            OutlinedButton(
                onClick = { onPasteInviteLink(clipboardManager.getText()?.text) },
                enabled = !state.isResolvingLink,
            ) {
                Text("Aus Zwischenablage einfügen")
            }
            if (selected != null) {
                TextButton(onClick = onCancelPicking) { Text("Abbrechen") }
            }
        }

        state.errorMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Erneut laden") }
        }
    }
}

@Composable
private fun OrgLevelDropdown(selection: OrgLevelSelection, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = !selection.isLoading && selection.options.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (selection.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    "${selection.level.label}: ${selection.selectedUnit?.displayName ?: "bitte wählen"}",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            selection.options.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.displayName) },
                    onClick = {
                        expanded = false
                        onSelect(unit.id)
                    },
                )
            }
        }
    }
}

/** Verbindet den [CongregationSelector] mit seinem ViewModel. */
@Composable
internal fun CongregationSelector(
    viewModel: CongregationSelectionViewModel,
    state: CongregationSelectionUiState,
    modifier: Modifier = Modifier,
) {
    CongregationSelector(
        state = state,
        onStartPicking = viewModel::startPicking,
        onCancelPicking = viewModel::cancelPicking,
        onSelectUnit = viewModel::selectUnit,
        onInviteLinkChange = viewModel::onInviteLinkChanged,
        onApplyInviteLink = { viewModel.applyInviteLink() },
        onPasteInviteLink = viewModel::pasteInviteLink,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

private val previewRef = CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin-mitte")

@ThemePreviews
@Composable
private fun CongregationSelectorPickingPreview() = PreviewThemeWrapper {
    CongregationSelector(
        state = CongregationSelectionUiState(
            levels = listOf(
                OrgLevelSelection(OrgLevel.REGION, listOf(OrgUnit("EMEA")), selectedId = "EMEA"),
                OrgLevelSelection(OrgLevel.SUBREGION, listOf(OrgUnit("europe", "Europa"))),
            ),
        ),
        onStartPicking = {},
        onCancelPicking = {},
        onSelectUnit = { _, _ -> },
        onInviteLinkChange = {},
        onApplyInviteLink = {},
        onPasteInviteLink = {},
        onRetry = {},
    )
}

@ThemePreviews
@Composable
private fun CongregationSelectorSelectedPreview() = PreviewThemeWrapper {
    CongregationSelector(
        state = CongregationSelectionUiState(selected = SelectedCongregation(previewRef, "Berlin-Mitte")),
        onStartPicking = {},
        onCancelPicking = {},
        onSelectUnit = { _, _ -> },
        onInviteLinkChange = {},
        onApplyInviteLink = {},
        onPasteInviteLink = {},
        onRetry = {},
    )
}
