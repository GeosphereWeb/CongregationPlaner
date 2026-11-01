package de.geosphere.congregationplaner.presentation.navigation

import dev.zacsweers.metro.DependencyGraph

@DependencyGraph
interface AppNavigationGraph {
    val appNavigationViewModel: AppNavigationViewModel
}
