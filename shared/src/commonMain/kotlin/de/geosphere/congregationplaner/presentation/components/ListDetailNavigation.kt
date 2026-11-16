package de.geosphere.congregationplaner.presentation.components

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Zustand einer Listen-/Detailansicht, den [rememberListDetailNavigation] braucht. */
interface ListDetailState {
    /** `true`, solange ein Eintrag im Detail geöffnet ist. */
    val hasDetail: Boolean
    val isSaving: Boolean
    val errorMessage: String?
}

/**
 * Hält die Navigation einer Listen-/Detailansicht synchron zum [state]: ein geöffneter Eintrag zeigt das Detail,
 * System-Zurück und ein erfolgreiches Speichern rufen auf kleinen Bildschirmen (eine Spalte) [onClose] auf.
 * Auf großen Bildschirmen stehen Liste und Detail nebeneinander, dort gibt es nichts zu navigieren.
 */
@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun rememberListDetailNavigation(
    state: ListDetailState,
    onClose: () -> Unit,
): ThreePaneScaffoldNavigator<Nothing> {
    val hasDetail = state.hasDetail
    val isSaving = state.isSaving
    val hasError = state.errorMessage != null
    val navigator = rememberListDetailPaneScaffoldNavigator<Nothing>(
        scaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()).withoutAutoFocus(),
        initialDestinationHistory = listOf(ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List)),
    )
    val singlePane = navigator.scaffoldDirective.maxHorizontalPartitions == 1

    LaunchedEffect(hasDetail, singlePane) {
        if (!singlePane) return@LaunchedEffect
        if (hasDetail) {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
        } else if (navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail) {
            navigator.navigateBack()
        }
    }
    // System-Zurück im Scaffold bedeutet: Detail schließen
    LaunchedEffect(navigator.currentDestination?.pane) {
        if (singlePane && navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.List && hasDetail) onClose()
    }
    // Nach erfolgreichem Speichern zurück zur Liste, wenn diese sonst verdeckt wäre
    var wasSaving by remember { mutableStateOf(false) }
    LaunchedEffect(isSaving) {
        val saveFinished = wasSaving && !isSaving
        if (saveFinished && !hasError && singlePane) onClose()
        wasSaving = isSaving
    }
    return navigator
}

/**
 * Ohne das würde der Scaffold bei jedem Panewechsel das neue Pane fokussieren; Compose gibt den Fokus dann an das erste
 * Textfeld weiter (Suchfeld der Liste, erstes Feld im Detail) und die Tastatur geht auf.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun PaneScaffoldDirective.withoutAutoFocus() = PaneScaffoldDirective(
    maxHorizontalPartitions = maxHorizontalPartitions,
    horizontalPartitionSpacerSize = horizontalPartitionSpacerSize,
    maxVerticalPartitions = maxVerticalPartitions,
    verticalPartitionSpacerSize = verticalPartitionSpacerSize,
    defaultPanePreferredWidth = defaultPanePreferredWidth,
    defaultPanePreferredHeight = defaultPanePreferredHeight,
    excludedBounds = excludedBounds,
    shouldAutoFocusCurrentDestination = false,
)
