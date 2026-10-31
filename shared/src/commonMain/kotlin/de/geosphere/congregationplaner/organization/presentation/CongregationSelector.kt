package de.geosphere.congregationplaner.organization.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.model.SelectedCongregation
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.NightPhonePreview
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightTextStyle
import de.geosphere.congregationplaner.ui.button.PrimaryButton
import de.geosphere.congregationplaner.ui.button.SecondaryButton
import de.geosphere.congregationplaner.ui.field.FieldBox
import de.geosphere.congregationplaner.ui.org.OrgLevelRow
import de.geosphere.congregationplaner.ui.org.OrgLevelStatus

/**
 * Auswahl der Versammlung im Login: entweder Ebene für Ebene (Region → … → Versammlung)
 * oder über einen Einladungslink des Versammlungsadmins.
 */
@Composable
@Suppress("LongParameterList", "DEPRECATION", "LongMethod", "MagicNumber")
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
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val selected = state.selected
        if (selected != null && !state.isPicking) {
            SelectedCongregationCard(selected, onChange = onStartPicking)
            return@Column
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            state.levels.forEach { level ->
                OrgLevelDropdown(selection = level, onSelect = { unitId -> onSelectUnit(level.level, unitId) })
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = NightColors.Border)
            Text(
                "oder mit Einladungslink",
                style = nightTextStyle(13.sp, FontWeight.Medium, NightColors.OnBackgroundMuted),
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = NightColors.Border)
        }
        FieldBox(
            label = "Einladungslink vom Versammlungsadmin",
            value = state.inviteLinkInput,
            onValueChange = onInviteLinkChange,
            placeholder = "${CongregationRef.INVITE_LINK_PREFIX}…",
            monospace = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        val clipboardManager = LocalClipboardManager.current
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                text = if (state.isResolvingLink) "Prüft …" else "Link übernehmen",
                onClick = onApplyInviteLink,
                modifier = Modifier.weight(1f),
                enabled = state.inviteLinkInput.isNotBlank() && !state.isResolvingLink,
                height = 48.dp,
            )
            SecondaryButton(
                text = "Einfügen",
                onClick = { onPasteInviteLink(clipboardManager.getText()?.text) },
                enabled = !state.isResolvingLink,
                contentColor = NightColors.Lavender,
                height = 48.dp,
            )
        }
        if (selected != null) {
            SecondaryButton("Abbrechen", onCancelPicking, Modifier.fillMaxWidth(), outlined = false)
        }

        state.errorMessage?.let { message ->
            Text(message, style = nightTextStyle(14.sp, color = NightColors.Coral))
            SecondaryButton("Erneut laden", onRetry, Modifier.fillMaxWidth(), outlined = false)
        }
    }
}

/** Gewählte Versammlung mit Kreis und Möglichkeit, sie zu ändern. */
@Composable
@Suppress("MagicNumber")
private fun SelectedCongregationCard(selected: SelectedCongregation, onChange: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier.fillMaxWidth().background(NightColors.Surface, shape)
            .border(1.dp, NightColors.Border, shape).padding(start = 16.dp, top = 10.dp, end = 6.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "VERSAMMLUNG",
                style = nightTextStyle(
                    11.sp,
                    color = NightColors.OnBackgroundMuted,
                    monospace = true,
                    letterSpacing = 0.88.sp,
                ),
            )
            Row {
                Text(selected.displayName, style = nightTextStyle(16.sp, FontWeight.Bold))
                Text(
                    " · Kreis ${selected.ref.circuitId}",
                    style = nightTextStyle(16.sp, color = NightColors.OnBackgroundMuted),
                )
            }
        }
        SecondaryButton("Ändern", onChange, outlined = false)
    }
}

@Composable
private fun OrgLevelDropdown(selection: OrgLevelSelection, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val status = when {
        selection.isLoading -> OrgLevelStatus.Loading
        selection.selectedUnit != null -> OrgLevelStatus.Done
        else -> OrgLevelStatus.Active
    }
    Box {
        OrgLevelRow(
            label = selection.level.label,
            value = selection.selectedUnit?.displayName,
            status = status,
            onClick = { expanded = true },
            enabled = !selection.isLoading && selection.options.isNotEmpty(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(NightColors.Surface),
        ) {
            selection.options.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.displayName, style = nightTextStyle(15.sp, FontWeight.Medium)) },
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

@Composable
private fun SelectorPreview(state: CongregationSelectionUiState) = NightPhonePreview {
    Box(Modifier.padding(24.dp).size(342.dp, 520.dp)) {
        CongregationSelector(
            state = state,
            onStartPicking = {},
            onCancelPicking = {},
            onSelectUnit = { _, _ -> },
            onInviteLinkChange = {},
            onApplyInviteLink = {},
            onPasteInviteLink = {},
            onRetry = {},
        )
    }
}

/** Auswahl läuft: erledigte Ebenen mit Häkchen, die nächste lädt noch. */
@ThemePreviews
@Composable
private fun CongregationSelectorPickingPreview() = SelectorPreview(
    CongregationSelectionUiState(
        levels = listOf(
            OrgLevelSelection(OrgLevel.REGION, listOf(OrgUnit("EMEA")), selectedId = "EMEA"),
            OrgLevelSelection(OrgLevel.SUBREGION, listOf(OrgUnit("europe", "Europa")), selectedId = "europe"),
            OrgLevelSelection(OrgLevel.BRANCH, isLoading = true),
        ),
    ),
)

/** Versammlung gewählt. */
@ThemePreviews
@Composable
private fun CongregationSelectorSelectedPreview() = SelectorPreview(
    CongregationSelectionUiState(
        selected = SelectedCongregation(
            CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin-mitte"),
            "Berlin-Mitte",
        ),
    ),
)
