package de.geosphere.congregationplaner.notice.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import de.geosphere.congregationplaner.notice.di.TalkNoticeGraph
import de.geosphere.congregationplaner.presentation.components.rememberScopedViewModel
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.createGraph

/** The talk notice section (PDF) for talk planners; shows nothing for everyone else. */
@Composable
fun TalkNoticeRoute(currentUser: AppUser, modifier: Modifier = Modifier) {
    val viewModel = rememberScopedViewModel { createGraph<TalkNoticeGraph>().talkNoticeViewModel }
    LaunchedEffect(viewModel, currentUser) { viewModel.onCurrentUserChanged(currentUser) }
    val state by viewModel.uiState.collectAsState()
    TalkNoticeContent(
        state,
        TalkNoticeActions(
            onChangeStart = viewModel::changeStart,
            onDecreaseCount = viewModel::decreaseCount,
            onIncreaseCount = viewModel::increaseCount,
            onSelectImage = viewModel::selectImage,
            onCreatePdf = viewModel::createPdf,
            onDismissSaved = viewModel::dismissSaved,
            onDismissError = viewModel::dismissError,
            onEditTitle = viewModel::editTitle,
            onSaveTitle = viewModel::saveTitle,
            onChooseColorSet = viewModel::chooseColorSet,
            onMakeDefault = viewModel::makeDefault,
            onAddImage = viewModel::addImage,
            onRenameImage = viewModel::renameImage,
            isValidImageName = viewModel::isValidImageName,
            onRequestDeleteImage = viewModel::requestDeleteImage,
            onConfirmDeleteImage = viewModel::confirmDeleteImage,
        ),
        modifier,
    )
}
