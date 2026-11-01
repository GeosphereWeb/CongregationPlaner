package de.geosphere.congregationplaner.congregation.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import de.geosphere.congregationplaner.congregation.di.CongregationMetadataGraph
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.createGraph

@Composable
internal fun CongregationMetadataRoute(currentUser: AppUser, ownOnly: Boolean = false) {
    val viewModel = remember { createGraph<CongregationMetadataGraph>().congregationMetadataViewModel }
    LaunchedEffect(viewModel, currentUser, ownOnly) { viewModel.onCurrentUserChanged(currentUser, ownOnly) }
    CongregationMetadataScreen(viewModel)
}
