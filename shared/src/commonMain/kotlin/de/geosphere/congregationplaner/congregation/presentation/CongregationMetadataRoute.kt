package de.geosphere.congregationplaner.congregation.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import de.geosphere.congregationplaner.congregation.di.CongregationMetadataGraph
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.createGraph

/** Mit [ownOnly] die eigene Versammlung des Nutzers, sonst die Versammlungen des Kreises [circuitIds]. */
@Composable
internal fun CongregationMetadataRoute(
    currentUser: AppUser,
    ownOnly: Boolean = false,
    circuitIds: List<String>? = null,
) {
    val viewModel = remember { createGraph<CongregationMetadataGraph>().congregationMetadataViewModel }
    LaunchedEffect(viewModel, currentUser, ownOnly, circuitIds) {
        viewModel.onCurrentUserChanged(currentUser, ownOnly, circuitIds)
    }
    CongregationMetadataScreen(viewModel)
}
