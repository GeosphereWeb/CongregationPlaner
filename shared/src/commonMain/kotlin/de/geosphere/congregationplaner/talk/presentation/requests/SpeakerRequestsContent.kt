package de.geosphere.congregationplaner.talk.presentation.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.talk.domain.model.ContactAttempt
import de.geosphere.congregationplaner.talk.domain.model.ContactKind
import de.geosphere.congregationplaner.talk.domain.model.OpenRequest
import de.geosphere.congregationplaner.talk.domain.model.SpeakerRequest
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.button.PrimaryButton
import de.geosphere.congregationplaner.ui.list.SelectableListCard
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@Composable
internal fun SpeakerRequestsContent(
    state: SpeakerRequestsUiState,
    actions: SpeakerRequestsActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.canEdit) {
            item {
                PrimaryButton("Anfrage ohne Tag", actions.onNew, Modifier.fillMaxWidth(), enabled = !state.isSaving)
            }
        }
        state.errorMessage?.takeIf { state.dialog == null }?.let { message ->
            item {
                Row {
                    Text(message, color = NightColors.Coral, modifier = Modifier.weight(1f))
                    TextButton(onClick = actions.onDismissError) { Text("OK") }
                }
            }
        }
        if (!state.isLoading && state.items.isEmpty()) {
            item { Text("Keine offenen Anfragen.", style = MaterialTheme.typography.bodyMedium) }
        }
        items(state.items, key = { it.request.id }) { open ->
            RequestCard(open, canEdit = state.canEdit, isSaving = state.isSaving, actions = actions)
        }
    }
    RequestDialogs(state, actions)
}

@Composable
private fun RequestCard(open: OpenRequest, canEdit: Boolean, isSaving: Boolean, actions: SpeakerRequestsActions) {
    SelectableListCard(
        selected = false,
        onSelect = {},
        onDelete = { actions.onAskDecline(open) },
        actionsEnabled = !isSaving,
        canDelete = canEdit,
        highlight = requestHighlight(open),
    ) {
        Text(open.speakerDisplayName ?: "Ohne Redner", fontWeight = FontWeight.SemiBold)
        Text(requestDayText(open), style = MaterialTheme.typography.bodyMedium)
        open.talkLabel?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Text(requestStatusText(open.request), style = MaterialTheme.typography.bodySmall)
        if (canEdit) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { actions.onContact(open) }, enabled = !isSaving) { Text("Kontakt") }
                if (open.slot != null) {
                    TextButton(onClick = { actions.onAccept(open) }, enabled = !isSaving) { Text("Zugesagt") }
                } else {
                    TextButton(onClick = { actions.onAssign(open) }, enabled = !isSaving) { Text("Tag zuweisen") }
                }
            }
        }
    }
}

private val previewToday = LocalDate(2026, 11, 24)

private val previewItems = listOf(
    OpenRequest(
        SpeakerRequest(
            "r1",
            LocalDate(2026, 11, 10),
            contactAttempts = listOf(ContactAttempt(LocalDate(2026, 11, 12), ContactKind.VOICEMAIL)),
        ),
        TalkSlot(
            LocalDate(2026, 12, 6),
            LocalTime(10, 0),
            talkNumber = 12,
            talkTitle = "Der Weg zum Glück",
            speakerMemberId = "m1",
            speakerLastName = "Muster",
            speakerFirstName = "Max",
            requestId = "r1",
        ),
        needsFollowUp = true,
        datePassed = false,
    ),
    OpenRequest(
        SpeakerRequest("u1", LocalDate(2026, 11, 20), speakerLastName = "Huber (Nord)", speakerFirstName = ""),
        slot = null,
        needsFollowUp = false,
        datePassed = false,
    ),
)

@ThemePreviews
@Composable
private fun SpeakerRequestsPlannerPreview() = PreviewThemeWrapper {
    SpeakerRequestsContent(
        SpeakerRequestsUiState(previewToday, previewItems, canEdit = true, isLoading = false),
        SpeakerRequestsActions(),
    )
}

@ThemePreviews
@Composable
private fun SpeakerRequestsHelperPreview() = PreviewThemeWrapper {
    SpeakerRequestsContent(
        SpeakerRequestsUiState(previewToday, previewItems, isLoading = false),
        SpeakerRequestsActions(),
    )
}

@ThemePreviews
@Composable
private fun SpeakerRequestsEmptyPreview() = PreviewThemeWrapper {
    SpeakerRequestsContent(
        SpeakerRequestsUiState(previewToday, canEdit = true, isLoading = false, errorMessage = "Keine Verbindung."),
        SpeakerRequestsActions(),
    )
}
