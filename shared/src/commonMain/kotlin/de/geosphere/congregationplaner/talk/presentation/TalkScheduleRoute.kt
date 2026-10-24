package de.geosphere.congregationplaner.talk.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import de.geosphere.congregationplaner.talk.di.TalkScheduleGraph
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.createGraph

@Composable
internal fun TalkScheduleRoute(currentUser: AppUser) {
    val viewModel = remember { createGraph<TalkScheduleGraph>().talkScheduleViewModel }
    LaunchedEffect(viewModel, currentUser) { viewModel.onCurrentUserChanged(currentUser) }
    TalkScheduleScreen(viewModel)
}
