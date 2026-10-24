package de.geosphere.congregationplaner.talk.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import de.geosphere.congregationplaner.talk.di.TalkReferenceGraph
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.createGraph

@Composable
internal fun TalkReferenceRoute(currentUser: AppUser) {
    val graph = remember { createGraph<TalkReferenceGraph>() }
    val catalogViewModel = remember { graph.talkCatalogManagementViewModel }
    val eventViewModel = remember { graph.weekEventManagementViewModel }
    LaunchedEffect(catalogViewModel, eventViewModel, currentUser) {
        catalogViewModel.onCurrentUserChanged(currentUser)
        eventViewModel.onCurrentUserChanged(currentUser)
    }
    TalkReferenceScreen(catalogViewModel, eventViewModel)
}
