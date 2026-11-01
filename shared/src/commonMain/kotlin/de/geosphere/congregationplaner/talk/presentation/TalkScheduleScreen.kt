package de.geosphere.congregationplaner.talk.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
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
import de.geosphere.congregationplaner.ui.button.SecondaryButton
import de.geosphere.congregationplaner.ui.card.DateBadge
import de.geosphere.congregationplaner.ui.chip.StatusChip
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

        if (state.isLoading) {
            CircularProgressIndicator()
            return@Column
        }

        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            scaffoldState = navigator.scaffoldState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            listPane = {
                AnimatedPane {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            SecondaryButton(
                                text = "Termin anlegen",
                                onClick = actions.onNewSlot,
                                enabled = !state.isSaving,
                                leading = { Icon(addIcon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            )
                        }
                        if (state.slots.isEmpty()) {
                            item { Text("Für ${state.year} sind noch keine Termine angelegt.") }
                        }
                        state.slots.groupBy { it.date.month }.values.forEach { monthSlots ->
                            item(key = "month-${monthSlots.first().id}") {
                                SectionLabel(slotMonthLabel(monthSlots.first().date), Modifier.padding(top = 8.dp))
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
}

/** Datumsblock, Vortrag und Redner mit Herkunft sowie die Statusmarke. */
@Composable
private fun SlotSummary(slot: TalkSlot, chip: SlotChip) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DateBadge(germanWeekdayName(slot.date).take(WEEKDAY_SHORT_LENGTH), slot.date.day.toString())
        Column(Modifier.weight(1f)) {
            Text(slotTitle(slot), style = MaterialTheme.typography.titleMedium, color = NightColors.OnBackground)
            Text(
                slotSpeakerLine(slot),
                style = MaterialTheme.typography.bodySmall,
                color = NightColors.OnBackgroundMuted,
            )
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
