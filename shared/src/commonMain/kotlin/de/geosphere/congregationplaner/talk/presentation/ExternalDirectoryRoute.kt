package de.geosphere.congregationplaner.talk.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import de.geosphere.congregationplaner.talk.di.ExternalDirectoryGraph
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.createGraph

@Composable
internal fun ExternalDirectoryRoute(currentUser: AppUser) {
    val viewModel = remember { createGraph<ExternalDirectoryGraph>().externalDirectoryViewModel }
    LaunchedEffect(viewModel, currentUser) { viewModel.onCurrentUserChanged(currentUser) }
    ExternalDirectoryScreen(viewModel)
}
