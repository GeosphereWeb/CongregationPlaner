package de.geosphere.congregationplaner.notification.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.notification.di.NotificationGraph
import de.geosphere.congregationplaner.presentation.components.rememberScopedViewModel
import de.geosphere.congregationplaner.talk.presentation.reference.ErrorRow
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightCardColors
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import dev.zacsweers.metro.createGraph

@Composable
internal fun AnnouncementRoute(currentUser: AppUser) {
    val viewModel = rememberScopedViewModel { createGraph<NotificationGraph>().announcementViewModel }
    LaunchedEffect(viewModel, currentUser) { viewModel.onCurrentUserChanged(currentUser) }
    val state by viewModel.uiState.collectAsState()
    AnnouncementContent(
        state = state,
        onDraftChange = viewModel::updateDraft,
        onSend = viewModel::send,
        onDismissError = viewModel::dismissError,
    )
}

@Composable
internal fun AnnouncementContent(
    state: AnnouncementUiState,
    onDraftChange: (AnnouncementDraft) -> Unit = {},
    onSend: () -> Unit = {},
    onDismissError: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!state.canSend) {
            Text("Dafür fehlt dir die Berechtigung.")
            return@Column
        }
        ErrorRow(state.errorMessage, onDismissError)
        state.sentMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        AnnouncementForm(state, onDraftChange, onSend)
    }
}

@Composable
private fun AnnouncementForm(
    state: AnnouncementUiState,
    onDraftChange: (AnnouncementDraft) -> Unit,
    onSend: () -> Unit,
) {
    val draft = state.draft
    val enabled = !state.isSending
    Card(colors = nightCardColors()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Neue Mitteilung", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = draft.title,
                onValueChange = { onDraftChange(draft.copy(title = it)) },
                label = { Text("Titel") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = enabled,
            )
            OutlinedTextField(
                value = draft.body,
                onValueChange = { onDraftChange(draft.copy(body = it)) },
                label = { Text("Text") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = enabled,
            )
            Text("An Gruppen", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EventAudience.entries.forEach { group ->
                    FilterChip(
                        selected = group in draft.groups,
                        onClick = { onDraftChange(draft.toggled(group)) },
                        label = { Text(group.label) },
                        enabled = enabled,
                    )
                }
            }
            Text("An Rollen", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CongregationRole.entries.forEach { role ->
                    FilterChip(
                        selected = role in draft.roles,
                        onClick = { onDraftChange(draft.toggled(role)) },
                        label = { Text(role.label) },
                        enabled = enabled,
                    )
                }
            }
            Button(onClick = onSend, enabled = state.canSubmit) {
                Text(if (state.isSending) "Sendet …" else "Senden")
            }
        }
    }
}

private val previewDraft = AnnouncementDraft(
    title = "Kreiskongress",
    body = "Bitte bis Sonntag beim Ältesten anmelden.",
    groups = setOf(EventAudience.BROTHERS),
    roles = setOf(CongregationRole.TALK_PLANNER),
)

@ThemePreviews
@Composable
private fun AnnouncementContentPreview() = PreviewThemeWrapper {
    AnnouncementContent(AnnouncementUiState(canSend = true, draft = previewDraft))
}

@ThemePreviews
@Composable
private fun AnnouncementContentSentPreview() = PreviewThemeWrapper {
    AnnouncementContent(AnnouncementUiState(canSend = true, sentMessage = "Mitteilung gesendet."))
}

@ThemePreviews
@Composable
private fun AnnouncementContentSendingErrorPreview() = PreviewThemeWrapper {
    AnnouncementContent(
        AnnouncementUiState(canSend = true, draft = previewDraft, isSending = true, errorMessage = "Keine Verbindung."),
    )
}

@ThemePreviews
@Composable
private fun AnnouncementContentNotAllowedPreview() = PreviewThemeWrapper {
    AnnouncementContent(AnnouncementUiState())
}
