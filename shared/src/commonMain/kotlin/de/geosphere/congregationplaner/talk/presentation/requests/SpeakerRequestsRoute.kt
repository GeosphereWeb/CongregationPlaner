package de.geosphere.congregationplaner.talk.presentation.requests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import de.geosphere.congregationplaner.talk.di.SpeakerRequestsGraph
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.createGraph

@Composable
internal fun SpeakerRequestsRoute(currentUser: AppUser) {
    val viewModel = remember { createGraph<SpeakerRequestsGraph>().speakerRequestsViewModel }
    LaunchedEffect(viewModel, currentUser) { viewModel.onCurrentUserChanged(currentUser) }
    val state by viewModel.uiState.collectAsState()
    SpeakerRequestsContent(
        state = state,
        actions = SpeakerRequestsActions(
            onNew = viewModel.draft::newRequest,
            onContact = viewModel.changes::openContact,
            onSaveContact = viewModel.changes::saveContact,
            onAccept = viewModel.changes::accept,
            onAskDecline = viewModel.changes::askDecline,
            onConfirmDecline = viewModel.changes::confirmDecline,
            onAssign = viewModel.changes::openAssign,
            onPickDay = viewModel.changes::assign,
            onSelectSpeaker = viewModel.draft::selectSpeaker,
            onSelectExternal = viewModel.draft::selectExternalSpeaker,
            onFreeSpeaker = viewModel.draft::selectFreeSpeaker,
            onSelectTalk = viewModel.draft::selectTalk,
            onFreeTalk = viewModel.draft::selectFreeTalk,
            onNoteChange = viewModel.draft::updateNote,
            onSaveNew = viewModel.draft::saveNew,
            onDismissDialog = viewModel::dismissDialog,
            onDismissError = viewModel::dismissError,
        ),
    )
}
