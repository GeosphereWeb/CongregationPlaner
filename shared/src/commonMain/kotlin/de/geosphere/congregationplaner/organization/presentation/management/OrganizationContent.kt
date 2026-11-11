package de.geosphere.congregationplaner.organization.presentation.management

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import de.geosphere.congregationplaner.congregation.presentation.CongregationMetadataRoute
import de.geosphere.congregationplaner.organization.di.OrganizationManagementGraph
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.presentation.components.rememberListDetailNavigation
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightCardColors
import de.geosphere.congregationplaner.ui.list.SelectableListCard
import de.geosphere.congregationplaner.ui.pane.DetailPane
import de.geosphere.congregationplaner.ui.pane.DetailPlaceholder
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.createGraph

/** Mehrzahl der Ebene für Listen, Pfadzeile und den Knopf zur nächsten Ebene. */
internal val OrgLevel.pluralLabel: String
    get() = when (this) {
        OrgLevel.REGION -> "Regionen"
        OrgLevel.SUBREGION -> "Subregionen"
        OrgLevel.BRANCH -> "Zweige"
        OrgLevel.COUNTRY -> "Länder"
        OrgLevel.CIRCUIT -> "Kreise"
        OrgLevel.CONGREGATION -> "Versammlungen"
    }

@Composable
internal fun OrganizationRoute(currentUser: AppUser) {
    val viewModel = remember { createGraph<OrganizationManagementGraph>().organizationViewModel }
    LaunchedEffect(viewModel, currentUser) { viewModel.onCurrentUserChanged(currentUser) }
    val state by viewModel.uiState.collectAsState()
    // Eine Ebene nach oben, solange kein Eintrag geöffnet ist; das Detail schließt die Listenansicht selbst.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.path.isNotEmpty() && state.editor == null,
        onBackCompleted = { viewModel.back() },
    )
    Column(Modifier.fillMaxSize()) {
        // Den Titel „Organisation“ zeigt schon der Bereichskopf; hier steht nur der Pfad.
        // TextButton bringt 12 dp Innenabstand mit; 4 + 12 ergibt dieselbe 16-dp-Kante wie der Inhalt darunter.
        Column(Modifier.padding(start = 4.dp, top = 8.dp, end = 16.dp)) {
            PathBar(state.path, onGoTo = viewModel::goTo)
        }
        if (state.level == OrgLevel.CONGREGATION) {
            CongregationMetadataRoute(currentUser, circuitIds = state.parentIds)
        } else {
            OrganizationContent(
                state = state,
                actions = OrganizationActions(
                    onCreate = viewModel.editor::create,
                    onSelect = viewModel.editor::edit,
                    onOpen = viewModel::open,
                    onUpdate = viewModel.editor::update,
                    onSave = viewModel::save,
                    onCancel = viewModel.editor::cancel,
                    onDiscard = viewModel.editor::discard,
                    onDelete = viewModel::delete,
                ),
            )
        }
    }
}

/** Aktionen des Bereichs; gebündelt, damit die Composables überschaubare Signaturen behalten. */
internal data class OrganizationActions(
    val onCreate: () -> Unit = {},
    val onSelect: (OrgUnit) -> Unit = {},
    val onOpen: (OrgUnit) -> Unit = {},
    val onUpdate: (OrgUnit) -> Unit = {},
    val onSave: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onDiscard: () -> Unit = {},
    val onDelete: (OrgUnit) -> Unit = {},
)

/** „Regionen › EMEA › Europa“: jeder Eintrag außer dem letzten springt auf seine Ebene zurück. */
@Composable
private fun PathBar(path: List<OrgUnit>, onGoTo: (Int) -> Unit) {
    val labels = listOf(OrgLevel.REGION.pluralLabel) + path.map(OrgUnit::displayName)
    FlowRow(verticalArrangement = Arrangement.Center, itemVerticalAlignment = Alignment.CenterVertically) {
        labels.forEachIndexed { depth, label ->
            if (depth > 0) Text("›", color = NightColors.OnBackgroundMuted)
            val isCurrent = depth == labels.lastIndex
            TextButton(onClick = { onGoTo(depth) }, enabled = !isCurrent) {
                Text(label, color = if (isCurrent) NightColors.OnBackground else NightColors.Lavender)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Suppress("LongMethod")
internal fun OrganizationContent(
    state: OrganizationUiState,
    actions: OrganizationActions,
    modifier: Modifier = Modifier,
) {
    var unitToDelete by remember { mutableStateOf<OrgUnit?>(null) }
    val isBusy = state.isSaving || state.isDeleting
    val navigator = rememberListDetailNavigation(state, onClose = actions.onCancel)
    val singlePane = navigator.scaffoldDirective.maxHorizontalPartitions == 1
    val onCancelEditing = if (singlePane) actions.onCancel else actions.onDiscard
    val level = state.level

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                ListDetailPaneScaffold(
                    directive = navigator.scaffoldDirective,
                    scaffoldState = navigator.scaffoldState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    listPane = {
                        AnimatedPane {
                            OrgUnitList(
                                level = level,
                                units = state.units,
                                selectedId = state.editor?.original?.id,
                                actionsEnabled = !isBusy,
                                canManage = state.canManage,
                                onOpen = actions.onOpen,
                                onEdit = actions.onSelect,
                                onDelete = { unitToDelete = it },
                            )
                        }
                    },
                    detailPane = {
                        AnimatedPane {
                            DetailPane(
                                showBack = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] ==
                                    PaneAdaptedValue.Hidden,
                                onBack = actions.onCancel,
                                backEnabled = !isBusy,
                            ) {
                                val editor = state.editor
                                if (editor == null) {
                                    DetailPlaceholder(
                                        "Tippe auf einen Eintrag, um ihn zu öffnen; „Bearbeiten“ ändert seinen Namen.",
                                    )
                                } else {
                                    OrgUnitEditor(
                                        level = level,
                                        editor = editor,
                                        isSaving = state.isSaving,
                                        isBusy = isBusy,
                                        actions = actions.copy(onCancel = onCancelEditing),
                                    )
                                }
                            }
                        }
                    },
                )
            }
        }
        // Auf dem Handy verdeckt der Editor die Liste; dort würde der FAB ihn überlagern.
        if (state.canManage && (!singlePane || state.editor == null)) {
            FloatingActionButton(
                onClick = { if (!state.isLoading && !isBusy) actions.onCreate() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    .semantics { contentDescription = "${level.label} hinzufügen" },
            ) { Text("+", style = MaterialTheme.typography.headlineMedium) }
        }
    }

    unitToDelete?.let { unit ->
        AlertDialog(
            onDismissRequest = { if (!isBusy) unitToDelete = null },
            title = { Text("${level.label} löschen?") },
            text = {
                Text(
                    "„${unit.displayName}“ wird dauerhaft gelöscht. Das geht nur, solange darunter keine " +
                        "${checkNotNull(level.child).pluralLabel} mehr stehen.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        actions.onDelete(unit)
                        unitToDelete = null
                    },
                    enabled = !isBusy,
                ) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { unitToDelete = null }, enabled = !isBusy) { Text("Abbrechen") }
            },
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun OrgUnitList(
    level: OrgLevel,
    units: List<OrgUnit>,
    selectedId: String?,
    actionsEnabled: Boolean,
    canManage: Boolean,
    onOpen: (OrgUnit) -> Unit,
    onEdit: (OrgUnit) -> Unit,
    onDelete: (OrgUnit) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text(level.pluralLabel, style = MaterialTheme.typography.titleMedium) }
        if (units.isEmpty()) {
            item { Text("Noch keine ${level.pluralLabel} vorhanden.", color = NightColors.OnBackgroundMuted) }
        }
        // Ein Tipp öffnet die Ebene darunter; bearbeitet wird über die eigene Aktion.
        items(units, key = { it.id }) { unit ->
            SelectableListCard(
                selected = unit.id == selectedId,
                onSelect = { onOpen(unit) },
                onDelete = { onDelete(unit) },
                actionsEnabled = actionsEnabled,
                canDelete = canManage,
                onEdit = { onEdit(unit) }.takeIf { canManage },
            ) {
                Text("${unit.displayName}  ›", style = MaterialTheme.typography.titleMedium)
                if (unit.overseerName.isNotBlank()) {
                    Text("Kreisaufseher: ${unit.overseerName}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun OrgUnitEditor(
    level: OrgLevel,
    editor: OrgUnitEditorState,
    isSaving: Boolean,
    isBusy: Boolean,
    actions: OrganizationActions,
) {
    val unit = editor.unit
    Card(colors = nightCardColors()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (editor.isNew) "${level.label} anlegen" else "${level.label} bearbeiten",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = unit.name,
                onValueChange = { actions.onUpdate(unit.copy(name = it)) },
                label = { Text("Name") },
                singleLine = true,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
            )
            if (level == OrgLevel.CIRCUIT) {
                OutlinedTextField(
                    value = unit.overseerName,
                    onValueChange = { actions.onUpdate(unit.copy(overseerName = it)) },
                    label = { Text("Kreisaufseher (optional)") },
                    singleLine = true,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = actions.onSave, enabled = !isBusy && editor.isDirty) {
                    Text(if (isSaving) "Speichert …" else "Speichern")
                }
                TextButton(onClick = actions.onCancel, enabled = !isBusy) { Text("Abbrechen") }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun OrganizationContentPreview() = PreviewThemeWrapper {
    val circuits = listOf(OrgUnit("c1", "Kreis Nord", "Max Muster"), OrgUnit("c2", "Kreis Süd"))
    OrganizationContent(
        state = OrganizationUiState(
            path = listOf(OrgUnit("r", "EMEA"), OrgUnit("s", "Europa"), OrgUnit("b", "Mitteleuropa")),
            units = circuits,
            editor = OrgUnitEditorState(circuits.first()),
            isLoading = false,
            canManage = true,
        ),
        actions = OrganizationActions(),
    )
}
