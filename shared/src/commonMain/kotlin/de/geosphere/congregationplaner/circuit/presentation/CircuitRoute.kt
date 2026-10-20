package de.geosphere.congregationplaner.circuit.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import de.geosphere.congregationplaner.circuit.di.CircuitGraph
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.createGraph

@Composable
internal fun CircuitRoute(currentUser: AppUser) {
    val viewModel = remember { createGraph<CircuitGraph>().circuitViewModel }
    LaunchedEffect(viewModel, currentUser) { viewModel.onCurrentUserChanged(currentUser) }
    CircuitScreen(viewModel)
}
