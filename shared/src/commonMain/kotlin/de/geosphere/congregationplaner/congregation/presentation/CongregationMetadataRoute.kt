package de.geosphere.congregationplaner.congregation.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.geosphere.congregationplaner.congregation.di.CongregationMetadataGraph
import dev.zacsweers.metro.createGraph

@Composable
internal fun CongregationMetadataRoute() {
    val viewModel = remember { createGraph<CongregationMetadataGraph>().congregationMetadataViewModel }
    CongregationMetadataScreen(viewModel)
}
