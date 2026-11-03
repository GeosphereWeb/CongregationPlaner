package de.geosphere.congregationplaner.talk.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.geosphere.congregationplaner.presentation.components.DetailPane
import de.geosphere.congregationplaner.presentation.components.DetailPlaceholder
import de.geosphere.congregationplaner.presentation.components.ListPaneHeader
import de.geosphere.congregationplaner.presentation.components.SelectableListCard
import de.geosphere.congregationplaner.presentation.components.rememberListDetailNavigation
import de.geosphere.congregationplaner.presentation.navigation.germanWeekdayName
import de.geosphere.congregationplaner.talk.domain.model.Chairman
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.card.DateBadge
import de.geosphere.congregationplaner.ui.chip.StatusChip
import de.geosphere.congregationplaner.ui.chip.StatusTone
import de.geosphere.congregationplaner.ui.nav.SectionLabel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Callbacks der Vortragsplanung; gebündelt, damit die Composables überschaubare Signaturen behalten. */
internal data class TalkScheduleActions(
    val onYearChange: (Int) -> Unit = {},
    val onNewSlot: () -> Unit = {},
    val onEditSlot: (TalkSlot) -> Unit = {},
    val onEditorChange: (TalkSlot) -> Unit = {},
    val onSelectWeekEvent: (WeekEvent?) -> Unit = {},
    val onSelectTalk: (Talk?) -> Unit = {},
    val speaker: SpeakerActions = SpeakerActions(),
    val onSelectChairman: (Chairman?) -> Unit = {},
    val onOpenHistory: (Int) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: (TalkSlot) -> Unit = {},
    val onCancelEditing: () -> Unit = {},
    val onDiscardChanges: () -> Unit = {},
    val onDismissError: () -> Unit = {},
    val onAskCreateYear: () -> Unit = {},
    val onConfirmCreateYear: () -> Unit = {},
    val onDismissCreateYear: () -> Unit = {},
    val onDismissInfo: () -> Unit = {},
    val onToggleStatusFilter: (SlotStatus) -> Unit = {},
)

@Composable
fun TalkScheduleScreen(
    viewModel: TalkScheduleViewModel,
    modifier: Modifier = Modifier,
    onOpenHistory: (Int) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    TalkScheduleContent(
        state = state,
        modifier = modifier,
        actions = TalkScheduleActions(
            onYearChange = viewModel::changeYear,
            onNewSlot = viewModel::newSlot,
            onEditSlot = viewModel::editSlot,
            onEditorChange = viewModel::updateEditor,
            onSelectWeekEvent = viewModel::selectWeekEvent,
            onSelectTalk = viewModel::selectTalk,
            speaker = SpeakerActions(
                onSelectMember = viewModel::selectSpeaker,
                onSelectExternal = viewModel::selectExternalSpeaker,
            ),
            onSelectChairman = viewModel::selectChairman,
            onOpenHistory = onOpenHistory,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
            onCancelEditing = viewModel::cancelEditing,
            onDiscardChanges = viewModel::discardChanges,
            onDismissError = viewModel::dismissError,
            onAskCreateYear = viewModel::askCreateYear,
            onConfirmCreateYear = viewModel::createYear,
            onDismissCreateYear = viewModel::dismissCreateYear,
            onDismissInfo = viewModel::dismissInfo,
            onToggleStatusFilter = viewModel::toggleStatusFilter,
        ),
    )
}

@Composable
private fun rememberToday(): LocalDate = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Suppress("LongMethod")
internal fun TalkScheduleContent(
    state: TalkScheduleUiState,
    actions: TalkScheduleActions,
    modifier: Modifier = Modifier,
    today: LocalDate = rememberToday(),
) {
    val editor = state.editor
    val navigator = rememberListDetailNavigation(state, onClose = actions.onCancelEditing)
    val singlePane = navigator.scaffoldDirective.maxHorizontalPartitions == 1
    val onCancelEditing = if (singlePane) actions.onCancelEditing else actions.onDiscardChanges
    val nextDate = nextSlotDate(state.slots, today)

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ListPaneHeader(
            title = "Planung am Wochenende",
            subtitle = state.year.toString(),
            onPrevious = { actions.onYearChange(state.year - 1) },
            previousDescription = "Vorheriges Jahr",
            onNext = { actions.onYearChange(state.year + 1) },
            nextDescription = "Nächstes Jahr",
        )
        if (!state.canPlan) {
            Text("Dafür fehlt dir die Berechtigung.")
            return@Column
        }

        state.errorMessage?.let { message ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                TextButton(onClick = actions.onDismissError) { Text("OK") }
            }
        }

        state.infoMessage?.let { message ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(message, modifier = Modifier.weight(1f))
                TextButton(onClick = actions.onDismissInfo) { Text("OK") }
            }
        }

        if (state.isLoading) {
            CircularProgressIndicator()
            return@Column
        }

        StatusFilterRow(state.slots, state.statusFilter, actions.onToggleStatusFilter)
        val visibleSlots = filterSlots(state.slots, state.statusFilter)

        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            scaffoldState = navigator.scaffoldState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            listPane = {
                AnimatedPane {
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            // Platz unter dem letzten Termin, damit der FAB ihn nicht verdeckt.
                            contentPadding = PaddingValues(bottom = FAB_CLEARANCE),
                        ) {
                            if (state.slots.isEmpty()) {
                                item { Text("Für ${state.year} sind noch keine Termine angelegt.") }
                            } else if (visibleSlots.isEmpty()) {
                                item { Text("Keine Termine für diese Auswahl.") }
                            }
                            visibleSlots.groupBy { it.date.month }.values.forEach { monthSlots ->
                                // Der Monat bleibt beim Scrollen oben stehen; der Hintergrund verdeckt die Termine darunter.
                                stickyHeader(key = "month-${monthSlots.first().id}") {
                                    SectionLabel(
                                        slotMonthLabel(monthSlots.first().date),
                                        Modifier.monthHeader(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                                    )
                                }
                                items(monthSlots, key = { it.id }) { slot ->
                                    SelectableListCard(
                                        selected = editor?.original?.date == slot.date,
                                        onSelect = { actions.onEditSlot(slot) },
                                        onDelete = { actions.onDelete(slot) },
                                        actionsEnabled = !state.isSaving,
                                        canDelete = true,
                                    ) {
                                        SlotSummary(slot, slotChip(slot, slotStatus(slot, nextDate)))
                                    }
                                }
                            }
                        }
                        CreateSlotsFab(
                            year = state.year,
                            enabled = !state.isSaving,
                            onNewSlot = actions.onNewSlot,
                            onCreateYear = actions.onAskCreateYear,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        )
                    }
                }
            },
            detailPane = {
                AnimatedPane {
                    DetailPane(
                        showBack = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden,
                        onBack = actions.onCancelEditing,
                        backEnabled = !state.isSaving,
                    ) {
                        if (editor != null) {
                            TalkSlotEditor(
                                editor = editor,
                                chip = slotChip(editor.slot, slotStatus(editor.slot, nextDate)),
                                weekEvents = state.weekEvents,
                                talks = state.talks,
                                speakers = state.speakers,
                                chairmen = state.chairmen,
                                externalSpeakers = state.externalSpeakers,
                                canViewMembers = state.canViewMembers,
                                isSaving = state.isSaving,
                                onChange = actions.onEditorChange,
                                onSelectWeekEvent = actions.onSelectWeekEvent,
                                onSelectTalk = actions.onSelectTalk,
                                speakerActions = actions.speaker,
                                onSelectChairman = actions.onSelectChairman,
                                onOpenHistory = actions.onOpenHistory,
                                onSave = actions.onSave,
                                onCancel = onCancelEditing,
                            )
                        } else {
                            DetailPlaceholder("Wähle einen Termin zum Bearbeiten aus.")
                        }
                    }
                }
            },
        )
    }

    if (state.confirmCreateYear) {
        AlertDialog(
            onDismissRequest = actions.onDismissCreateYear,
            title = { Text("Alle Termine ${state.year} anlegen?") },
            text = {
                Text(
                    "Für jeden Tag am Wochenende des Jahres wird ein Termin angelegt (Wochentag und Uhrzeit aus den " +
                        "Versammlungsdaten). Bestehende Termine bleiben unverändert.",
                )
            },
            confirmButton = { TextButton(onClick = actions.onConfirmCreateYear) { Text("Anlegen") } },
            dismissButton = { TextButton(onClick = actions.onDismissCreateYear) { Text("Abbrechen") } },
        )
    }
}

private val FAB_CLEARANCE = 88.dp

/**
 * Zahl der Termine je Stand im angezeigten Jahr; jeder Stand ist ein Filter. Nicht gewählt sind alle neutral, ein
 * gewählter zeigt seine Farbe und ein Häkchen. Mehrere lassen sich zugleich wählen; ohne Auswahl zeigt die Liste das
 * ganze Jahr.
 */
@Composable
private fun StatusFilterRow(slots: List<TalkSlot>, filter: Set<SlotStatus>, onToggle: (SlotStatus) -> Unit) {
    val counts = statusCounts(slots)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FILTER_STATUSES.forEach { status ->
            val selected = status in filter
            val (container, border) = filterChipColors(status)
            FilterChip(
                selected = selected,
                onClick = { onToggle(status) },
                label = { Text("${statusLabel(status)} ${counts[status] ?: 0}", fontSize = 11.sp) },
                modifier = Modifier.height(28.dp),
                leadingIcon = if (selected) {
                    { Icon(checkIcon, contentDescription = null, modifier = Modifier.size(14.dp)) }
                } else {
                    null
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = NightColors.Surface,
                    labelColor = NightColors.OnBackgroundBody,
                    selectedContainerColor = container,
                    selectedLabelColor = NightColors.OnBackgroundBody,
                    selectedLeadingIconColor = NightColors.OnBackgroundBody,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = NightColors.Border,
                    selectedBorderColor = border,
                ),
            )
        }
    }
}

/** Fläche und Rand eines gewählten Filter-Chips: Koralle für „Nicht geplant“, neutral für „Geplant“, Blau für „Entfällt“. */
private fun filterChipColors(status: SlotStatus): Pair<Color, Color> = when (status) {
    SlotStatus.NOT_PLANNED -> NightColors.CoralContainer to NightColors.CoralBorder
    SlotStatus.CANCELLED -> NightColors.SkyContainer to NightColors.SkyBorder
    else -> NightColors.Surface to NightColors.Border
}

/** Volle Breite mit dem Seitenhintergrund, damit der klebende Monatskopf die Termine darunter verdeckt. */
@Composable
private fun Modifier.monthHeader(): Modifier =
    fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(vertical = 8.dp)

/**
 * FAB der Liste: öffnet darüber zwei beschriftete Knöpfe „Termin anlegen“ und „Alle Termine <Jahr> anlegen“
 * (Speed Dial). Das „+“ dreht sich dabei zum „×“; ein zweiter Tipp schließt wieder.
 */
@Composable
private fun CreateSlotsFab(
    year: Int,
    enabled: Boolean,
    onNewSlot: () -> Unit,
    onCreateYear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val open = expanded && enabled
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (open) {
            SpeedDialAction("Alle Termine $year anlegen") {
                expanded = false
                onCreateYear()
            }
            SpeedDialAction("Termin anlegen") {
                expanded = false
                onNewSlot()
            }
        }
        // Helles Lavendel statt des dunklen primaryContainer, damit der FAB vom Hintergrund absticht.
        FloatingActionButton(
            onClick = { if (enabled) expanded = !expanded },
            containerColor = NightColors.Lavender,
            contentColor = NightColors.HeaderTop,
        ) {
            Icon(
                addIcon,
                contentDescription = if (open) "Schließen" else "Termine anlegen",
                modifier = Modifier.rotate(if (open) FAB_OPEN_ROTATION else 0f),
            )
        }
    }
}

private const val FAB_OPEN_ROTATION = 45f

/** Beschrifteter Knopf über dem FAB, im kräftigen Lila der übrigen Knöpfe. */
@Composable
private fun SpeedDialAction(text: String, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = NightColors.Primary,
        contentColor = NightColors.OnBackground,
        icon = { Icon(addIcon, contentDescription = null) },
        text = { Text(text) },
    )
}

/** Datumsblock, Vortrag und Redner mit Herkunft sowie die Statusmarke. */
@Composable
private fun SlotSummary(slot: TalkSlot, chip: SlotChip) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DateBadge(germanWeekdayName(slot.date).take(WEEKDAY_SHORT_LENGTH), slot.date.day.toString())
        Column(Modifier.weight(1f)) {
            Text(slotTitle(slot), style = MaterialTheme.typography.titleMedium, color = NightColors.OnBackground)
            Text(
                slotSpeakerLine(slot),
                style = MaterialTheme.typography.bodySmall,
                color = NightColors.OnBackgroundMuted,
            )
            slotEventLine(slot)?.let { event ->
                Text(event, style = MaterialTheme.typography.bodySmall, color = StatusTone.Sky.content)
            }
        }
        StatusChip(chip.text, chip.tone)
    }
}

private const val WEEKDAY_SHORT_LENGTH = 2

// Das Plus aus den Material Symbols (Apache 2.0).
private val addIcon = ImageVector.Builder(
    name = "add",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = addPathNodes("M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"),
    fill = SolidColor(Color.Black),
).build()

// Das Häkchen aus den Material Symbols (Apache 2.0).
private val checkIcon = ImageVector.Builder(
    name = "check",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = addPathNodes("M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z"),
    fill = SolidColor(Color.Black),
).build()

private val previewToday = LocalDate(2026, 10, 20)

private val previewSlots = listOf(
    TalkSlot(LocalDate(2026, 10, 25), LocalTime(17, 30)),
    TalkSlot(
        LocalDate(2026, 11, 1),
        LocalTime(17, 30),
        weekEventId = "kongress",
        weekEventName = "Kongress",
        talkCancelled = true,
    ),
    TalkSlot(LocalDate(2026, 11, 8), LocalTime(17, 30), talkNumber = 12, talkTitle = "Der Weg zum Glück"),
    TalkSlot(
        LocalDate(2026, 11, 15),
        LocalTime(17, 30),
        talkNumber = 12,
        talkTitle = "Der Weg zum Glück",
        speakerMemberId = "1",
        speakerLastName = "Muster",
        speakerFirstName = "Max",
    ),
    TalkSlot(
        LocalDate(2026, 11, 22),
        LocalTime(17, 30),
        talkNumber = 5,
        talkTitle = "Ein anderer Vortrag",
        externalSpeakerId = "e1",
        speakerLastName = "Extern",
        speakerFirstName = "Eva",
        speakerCongregation = "Nord",
        confirmed = true,
    ),
)

@ThemePreviews
@Composable
private fun TalkScheduleContentPreview() = PreviewThemeWrapper {
    TalkScheduleContent(
        state = TalkScheduleUiState(year = 2026, slots = previewSlots, isLoading = false, canPlan = true),
        actions = TalkScheduleActions(),
        today = previewToday,
    )
}
