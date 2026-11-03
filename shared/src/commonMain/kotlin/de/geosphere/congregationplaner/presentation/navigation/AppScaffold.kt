package de.geosphere.congregationplaner.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
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
import de.geosphere.congregationplaner.ui.common.topCutoutInset
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.model.GERMAN_AREA_CODES_STAND
import de.geosphere.congregationplaner.user.presentation.OwnProfileSection

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
                TopLevelDestination.SETTINGS -> SettingsContent(currentUser, actions.onSignOut)
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
/**
 * Feste Kopfzeile mit der Überschrift und „Abmelden“ (Exit-Symbol, rechts); darunter scrollt der Rest, weil das
 * Profil mit Adresse und Notfallkontakten länger als der Bildschirm ist.
 */
private fun SettingsContent(currentUser: AppUser, onSignOut: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 24.dp, end = 12.dp, top = maxOf(16.dp, topCutoutInset()), bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Einstellungen", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = onSignOut) {
                Icon(logoutIcon, contentDescription = "Abmelden", tint = NightColors.Lavender)
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        ) {
            Text(
                "Hier erscheinen später die Einstellungen der App.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OwnProfileSection(currentUser, Modifier.padding(top = 24.dp).widthIn(max = 480.dp))
            DataSources(Modifier.padding(top = 32.dp).widthIn(max = 480.dp))
        }
    }
}

/** Quellenangaben für eingebundene Daten; die Datenlizenz Deutschland verlangt die Namensnennung. */
@Composable
private fun DataSources(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Datenquellen", style = MaterialTheme.typography.titleSmall)
        Text(
            "Vorwahlverzeichnis: Bundesnetzagentur, Stand $GERMAN_AREA_CODES_STAND. " +
                "Lizenz: Datenlizenz Deutschland – Namensnennung – Version 4.0. " +
                "Bearbeitet: nur die aktiven Vorwahlen, ohne Ortsnamen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Das Logout-Symbol aus den Material Symbols (Apache 2.0).
private val logoutIcon = ImageVector.Builder(
    name = "logout",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = addPathNodes(
        "M5,21C4.45,21 3.98,20.8 3.59,20.41C3.2,20.02 3,19.55 3,19V5C3,4.45 3.2,3.98 3.59,3.59C3.98,3.2 " +
            "4.45,3 5,3H12V5H5V19H12V21H5ZM16,17L14.63,15.55L17.18,13H9V11H17.18L14.63,8.45L16,7L21,12L16,17Z",
    ),
    fill = SolidColor(Color.Black),
).build()

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
