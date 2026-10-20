package de.geosphere.congregationplaner.user.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import de.geosphere.congregationplaner.user.di.UserManagementGraph
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.createGraph

@Composable
internal fun UserManagementRoute(currentUser: AppUser) {
    val viewModel = remember { createGraph<UserManagementGraph>().userManagementViewModel }
    LaunchedEffect(viewModel, currentUser) { viewModel.onCurrentUserChanged(currentUser) }
    UserManagementScreen(viewModel)
}
