package de.geosphere.congregationplaner.talk.presentation.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.event.presentation.eventTypeIcon
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.button.PrimaryButton
import de.geosphere.congregationplaner.ui.card.FieldCard
import de.geosphere.congregationplaner.ui.form.FieldActions
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.painterResource

private val ACTION_HEIGHT = 44.dp

/** Auswahl, Entfernen und Sprung einer Feldkarte des Editors. */
internal class CardActions(val onPick: () -> Unit, val onRemove: () -> Unit, val onOpen: () -> Unit)

/** Auswahl des Wochenereignisses als Menü: je Woche gibt es höchstens eines. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WeekEventDropdown(slot: TalkSlot, eventTypes: List<EventType>, onSelect: (EventType?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = weekEventSelection(slot),
            onValueChange = {},
            readOnly = true,
            label = { Text("Wochenereignis") },
            leadingIcon = slotEventIcon(slot, eventTypes)?.let { icon -> { WeekEventIcon(icon) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(NO_WEEK_EVENT) },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            eventTypes.forEach { event ->
                DropdownMenuItem(
                    text = { Text(event.name) },
                    leadingIcon = { WeekEventIcon(event.icon) },
                    onClick = {
                        onSelect(event)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun WeekEventIcon(icon: String) {
    Icon(painterResource(eventTypeIcon(icon)), contentDescription = null, Modifier.size(20.dp))
}

/** Der Vortrag; mit [historyTalk] führt ein Tipp auf die Karte zur Vortragshistorie. */
@Composable
internal fun TalkCard(
    slot: TalkSlot,
    talkText: String?,
    inactiveNumber: Int?,
    historyTalk: Int?,
    actions: CardActions,
) {
    FieldCard(
        label = "Vortrag",
        value = talkText ?: "Noch kein Vortrag gewählt",
        supporting = withLinkHint(
            if (slot.talkCancelled) {
                "Der Vortrag entfällt, eine Auswahl ist nicht nötig."
            } else {
                inactiveNumber?.let(::inactiveTalkHint)
            },
            historyTalk?.let { TALK_LINK_HINT },
        ),
        missing = talkText == null && !slot.talkCancelled,
        onClick = historyTalk?.let { actions.onOpen },
        action = {
            FieldActions(
                changeLabel = if (talkText == null) "Wählen" else "Ändern",
                enabled = !slot.talkCancelled,
                onChange = actions.onPick,
                onRemove = if (talkText != null) actions.onRemove else null,
            )
        },
    )
}

/** Der Redner; mit [directorySpeaker] führt ein Tipp auf die Karte zu seinem Eintrag in der Rednerliste. */
@Composable
internal fun SpeakerCard(
    slot: TalkSlot,
    speakerText: String?,
    supporting: String?,
    directorySpeaker: String?,
    actions: CardActions,
) {
    val speakerMissing = speakerText == null && !slot.talkCancelled
    FieldCard(
        label = "Redner",
        value = speakerText ?: "Noch kein Redner",
        supporting = withLinkHint(supporting, directorySpeaker?.let { SPEAKER_LINK_HINT }),
        missing = speakerMissing,
        onClick = directorySpeaker?.let { actions.onOpen },
        action = {
            if (speakerMissing) {
                PrimaryButton("Redner wählen", actions.onPick, height = ACTION_HEIGHT)
            } else {
                FieldActions(
                    changeLabel = if (speakerText == null) "Wählen" else "Ändern",
                    enabled = !slot.talkCancelled,
                    onChange = actions.onPick,
                    onRemove = if (speakerText != null) actions.onRemove else null,
                )
            }
        },
    )
}

@Composable
internal fun ChairmanCard(
    slot: TalkSlot,
    supporting: String?,
    canPick: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    FieldCard(
        label = "Vorsitz",
        value = slot.chairman?.displayName ?: "Noch kein Vorsitzender",
        supporting = supporting,
        action = {
            FieldActions(
                changeLabel = if (slot.chairman == null) "Wählen" else "Ändern",
                enabled = canPick,
                onChange = onPick,
                onRemove = if (slot.chairman != null) onRemove else null,
            )
        },
    )
}

@ThemePreviews
@Composable
private fun SlotEditorCardsPreview() = PreviewThemeWrapper {
    val slot = TalkSlot(
        LocalDate(2026, 11, 22),
        LocalTime(17, 30),
        talkNumber = 12,
        talkTitle = "Der Weg zum Glück",
        externalSpeakerId = "e1",
        speakerLastName = "Extern",
        speakerFirstName = "Eva",
        speakerCongregation = "Nord",
    )
    val actions = CardActions({}, {}, {})
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val kongress = EventType("kongress", "Kongress", cancelsTalk = true, icon = "ic_cp_arena_duo")
        WeekEventDropdown(slot, listOf(kongress), {})
        WeekEventDropdown(slot.copy(weekEventId = "kongress", weekEventName = "Kongress"), listOf(kongress), {})
        // Gespeichert: beide Karten führen weiter.
        TalkCard(slot, slotTalkText(slot), inactiveNumber = null, historyTalk = 12, actions = actions)
        SpeakerCard(slot, slotSpeakerText(slot), supporting = null, directorySpeaker = "e1", actions = actions)
        // Ungespeichert bzw. leer: kein Sprung, der Redner fehlt.
        TalkCard(slot, slotTalkText(slot), inactiveNumber = 12, historyTalk = null, actions = actions)
        SpeakerCard(
            slot,
            speakerText = null,
            supporting = speakerSupporting(canViewMembers = true, noSpeakers = false, speakerMissing = true),
            directorySpeaker = null,
            actions = actions,
        )
        ChairmanCard(
            slot,
            supporting = chairmanSupporting(canViewMembers = true, noChairmen = true, noneSelectable = true),
            canPick = false,
            onPick = {},
            onRemove = {},
        )
    }
}
