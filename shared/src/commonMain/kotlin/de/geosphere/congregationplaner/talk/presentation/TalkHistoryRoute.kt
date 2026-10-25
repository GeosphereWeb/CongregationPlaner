package de.geosphere.congregationplaner.talk.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import de.geosphere.congregationplaner.talk.di.TalkHistoryGraph
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.createGraph

@Composable
internal fun TalkHistoryRoute(currentUser: AppUser) {
    val viewModel = remember { createGraph<TalkHistoryGraph>().talkHistoryViewModel }
    LaunchedEffect(viewModel, currentUser) { viewModel.onCurrentUserChanged(currentUser) }
    TalkHistoryScreen(viewModel)
}
