package de.geosphere.congregationplaner.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import de.geosphere.congregationplaner.theming.AppTheme
import de.geosphere.congregationplaner.theming.brushes.backgroundBrush
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole

/**
 * Der Rahmen der angemeldeten App mit den vier Hauptbereichen. Die Leiste liegt auf schmalen Fenstern unten und auf
 * breiten seitlich; das entscheidet [NavigationSuiteScaffold] aus der Fenstergröße, nicht aus der Plattform.
 * Ab mittlerer Breite zeigen „Versammlung“ und „Globale Verwaltung“ eine feste zweite Spalte, davor eine Übersicht.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppScaffold(
    state: AppNavigationState,
    firebaseStatus: String,
    currentUser: AppUser,
    actions: AppNavigationActions,
) {
    val isWide = currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
    )
    // Unterbereich → Übersicht → Home; auf Home greift das System-Zurück.
    BackHandler(enabled = state.canGoBack) { actions.onBack() }
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            state.visible.topLevel.forEach { destination ->
                item(
                    selected = destination == state.destination,
                    onClick = { actions.onSelect(destination) },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = { Text(destination.label) },
                )
            }
        },
    ) {
        Box(Modifier.fillMaxSize().background(Brush.backgroundBrush)) {
            when (state.destination) {
                TopLevelDestination.HOME -> HomeContent(firebaseStatus)
                TopLevelDestination.CONGREGATION -> CongregationArea(state, currentUser, isWide, actions)
                TopLevelDestination.GLOBAL_ADMIN -> GlobalArea(state, currentUser, isWide, actions)
                TopLevelDestination.SETTINGS -> SettingsContent(actions.onSignOut)
            }
        }
    }
}

@Composable
private fun HomeContent(firebaseStatus: String) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(firebaseStatus, style = MaterialTheme.typography.bodySmall)
        Text("Home Content")
    }
}

@Composable
private fun SettingsContent(onSignOut: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings Content")
        Button(onClick = onSignOut, modifier = Modifier.padding(top = 16.dp)) { Text("Abmelden") }
    }
}

private val previewUser = AppUser(
    uid = "preview",
    approved = true,
    roles = setOf(CongregationRole.CONGREGATION_ADMIN, CongregationRole.TALK_PLANNER),
    isGlobalAdmin = true,
)

private val previewVisible = VisibleNavigation(CongregationSection.entries, GlobalSection.entries)

private val previewOverview = AppNavigationState(
    visible = previewVisible,
    destination = TopLevelDestination.CONGREGATION,
)

private val previewTalkTab = previewOverview.copy(congregationSection = CongregationSection.EXTERNAL_SPEAKERS)

@Preview(name = "Handy – Übersicht", widthDp = 360, heightDp = 720)
@Composable
fun AppScaffoldPhoneOverviewPreview() {
    AppTheme { AppScaffold(previewOverview, "Firebase verfügbar", previewUser, AppNavigationActions()) }
}

@Preview(name = "Handy – Vortragsplanung mit Tabs", widthDp = 360, heightDp = 720)
@Composable
fun AppScaffoldPhoneTabsPreview() {
    AppTheme { AppScaffold(previewTalkTab, "Firebase verfügbar", previewUser, AppNavigationActions()) }
}

@Preview(name = "Tablet – zweite Spalte", widthDp = 1000, heightDp = 700)
@Composable
fun AppScaffoldTabletPreview() {
    AppTheme { AppScaffold(previewOverview, "Firebase verfügbar", previewUser, AppNavigationActions()) }
}
