package de.geosphere.congregationplaner.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import de.geosphere.congregationplaner.home.presentation.HomeContent
import de.geosphere.congregationplaner.home.presentation.HomeUiState
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.organization.domain.model.SelectedCongregation
import de.geosphere.congregationplaner.theming.AppTheme
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.ui.button.SecondaryButton
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole

/**
 * Der Rahmen der angemeldeten App mit den vier Hauptbereichen. Die Leiste liegt auf schmalen Fenstern unten und auf
 * breiten seitlich; das entscheidet [NavigationSuiteScaffold] aus der Fenstergröße, nicht aus der Plattform.
 * Ab mittlerer Breite zeigen „Versammlung“ und „Globale Verwaltung“ eine feste zweite Spalte, davor eine Übersicht.
 * [congregation] füllt die Kopfbereiche; globale Admins haben keine (ADR-0013).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppScaffold(
    state: AppNavigationState,
    firebaseStatus: String,
    currentUser: AppUser,
    congregation: SelectedCongregation?,
    home: HomeUiState,
    actions: AppNavigationActions,
) {
    val isWide = currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
    )
    // Unterbereich → Übersicht → Home; auf Home greift das System-Zurück.
    BackHandler(enabled = state.canGoBack) { actions.onBack() }
    val itemColors = nightNavigationItemColors()
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            state.visible.topLevel.forEach { destination ->
                val selected = destination == state.destination
                item(
                    selected = selected,
                    onClick = { actions.onSelect(destination) },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = {
                        Text(destination.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    },
                    colors = itemColors,
                )
            }
        },
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = NightColors.NavBar,
            navigationRailContainerColor = NightColors.HeaderTop,
        ),
    ) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (state.destination) {
                TopLevelDestination.HOME -> HomeContent(firebaseStatus, congregation, home, state.visible, actions)
                TopLevelDestination.CONGREGATION -> {
                    val pending = home.summary.pendingApprovals
                    CongregationArea(state, currentUser, isWide, congregation, pending, actions)
                }

                TopLevelDestination.GLOBAL_ADMIN -> GlobalArea(state, currentUser, isWide, congregation, actions)
                TopLevelDestination.SETTINGS -> SettingsContent(actions.onSignOut)
            }
        }
    }
}

/** Gewählt: Lavendel-Icon in der Pille `HeaderBottom`, fette Beschriftung; sonst gedämpft. */
@Composable
private fun nightNavigationItemColors() = NavigationSuiteDefaults.itemColors(
    navigationBarItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = NightColors.Lavender,
        selectedTextColor = NightColors.OnBackground,
        indicatorColor = NightColors.HeaderBottom,
        unselectedIconColor = NightColors.OnBackgroundMuted,
        unselectedTextColor = NightColors.OnBackgroundMuted,
    ),
    navigationRailItemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = NightColors.Lavender,
        selectedTextColor = NightColors.OnBackground,
        indicatorColor = NightColors.HeaderBottom,
        unselectedIconColor = NightColors.OnBackgroundMuted,
        unselectedTextColor = NightColors.OnBackgroundMuted,
    ),
)

@Composable
private fun SettingsContent(onSignOut: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Einstellungen", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Hier erscheinen später die Einstellungen der App.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        SecondaryButton(
            text = "Abmelden",
            onClick = onSignOut,
            outlined = false,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}

private val previewUser = AppUser(
    uid = "preview",
    approved = true,
    roles = setOf(CongregationRole.CONGREGATION_ADMIN, CongregationRole.TALK_PLANNER),
    isGlobalAdmin = true,
)

private val previewCongregation = SelectedCongregation(
    ref = CongregationRef("emea", "europe", "central", "609a", "berlin-mitte"),
    name = "Berlin-Mitte",
)

private val previewVisible = VisibleNavigation(CongregationSection.entries, GlobalSection.entries)

private val previewOverview = AppNavigationState(
    visible = previewVisible,
    destination = TopLevelDestination.CONGREGATION,
)

private val previewTalkTab = previewOverview.copy(congregationSection = CongregationSection.EXTERNAL_SPEAKERS)

@Composable
private fun ScaffoldPreview(state: AppNavigationState) = AppTheme {
    AppScaffold(
        state,
        "Firebase verfügbar",
        previewUser,
        previewCongregation,
        HomeUiState(),
        AppNavigationActions(),
    )
}

@Preview(name = "Handy – Übersicht", widthDp = 360, heightDp = 720)
@Composable
fun AppScaffoldPhoneOverviewPreview() = ScaffoldPreview(previewOverview)

@Preview(name = "Handy – Vortragsplanung mit Tabs", widthDp = 360, heightDp = 720)
@Composable
fun AppScaffoldPhoneTabsPreview() = ScaffoldPreview(previewTalkTab)

@Preview(name = "Handy – Home", widthDp = 360, heightDp = 720)
@Composable
fun AppScaffoldPhoneHomePreview() = ScaffoldPreview(previewOverview.copy(destination = TopLevelDestination.HOME))

@Preview(name = "Handy – Einstellungen", widthDp = 360, heightDp = 720)
@Composable
fun AppScaffoldPhoneSettingsPreview() =
    ScaffoldPreview(previewOverview.copy(destination = TopLevelDestination.SETTINGS))

@Preview(name = "Handy – nur Vortragsplaner", widthDp = 360, heightDp = 720)
@Composable
fun AppScaffoldPhoneTalkPlannerPreview() = ScaffoldPreview(
    previewOverview.copy(visible = VisibleNavigation(CongregationSection.talkPlanning)),
)

@Preview(name = "Handy – Globale Verwaltung", widthDp = 360, heightDp = 720)
@Composable
fun AppScaffoldPhoneGlobalPreview() = ScaffoldPreview(
    previewOverview.copy(destination = TopLevelDestination.GLOBAL_ADMIN),
)

@Preview(name = "Tablet – Planung am Wochenende gewählt", widthDp = 1000, heightDp = 700)
@Composable
fun AppScaffoldTabletPreview() = ScaffoldPreview(
    previewOverview.copy(congregationSection = CongregationSection.WEEKEND_TALKS),
)
