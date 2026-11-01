package de.geosphere.congregationplaner.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.circuit.presentation.CircuitRoute
import de.geosphere.congregationplaner.congregation.presentation.CongregationMetadataRoute
import de.geosphere.congregationplaner.talk.presentation.ExternalDirectoryRoute
import de.geosphere.congregationplaner.talk.presentation.TalkHistoryRoute
import de.geosphere.congregationplaner.talk.presentation.TalkReferenceRoute
import de.geosphere.congregationplaner.talk.presentation.TalkScheduleRoute
import de.geosphere.congregationplaner.theming.AppTheme
import de.geosphere.congregationplaner.theming.brushes.backgroundBrush
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.presentation.UserManagementRoute

/**
 * Der Rahmen der angemeldeten App mit den vier Hauptbereichen. Die Leiste liegt auf schmalen Fenstern unten und auf
 * breiten seitlich; das entscheidet [NavigationSuiteScaffold] aus der Fenstergröße, nicht aus der Plattform.
 */
@Composable
fun AppScaffold(
    state: AppNavigationState,
    firebaseStatus: String,
    currentUser: AppUser,
    actions: AppNavigationActions,
) {
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
                TopLevelDestination.CONGREGATION -> CongregationArea(state, currentUser, actions)
                TopLevelDestination.GLOBAL_ADMIN -> GlobalArea(state, currentUser, actions)
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

@Composable
private fun CongregationArea(state: AppNavigationState, currentUser: AppUser, actions: AppNavigationActions) {
    val section = state.congregationSection
    if (section == null) {
        CongregationOverview(state.visible.congregationSections, actions.onOpenCongregationSection)
    } else {
        SectionHost(onBack = { actions.onOpenCongregationSection(null) }) {
            when (section) {
                CongregationSection.GENERAL_DATA -> CongregationMetadataRoute(currentUser, ownOnly = true)
                CongregationSection.USER_MANAGEMENT -> UserManagementRoute(currentUser)
                CongregationSection.MIDWEEK_PLANNING ->
                    Text("leben_und_dienst \n Schätze \n uns verbessern \n leben als christ")
                CongregationSection.WEEKEND_TALKS -> TalkScheduleRoute(currentUser)
                CongregationSection.EXTERNAL_SPEAKERS -> ExternalDirectoryRoute(currentUser)
                CongregationSection.TALK_HISTORY -> TalkHistoryRoute(currentUser)
            }
        }
    }
}

@Composable
private fun GlobalArea(state: AppNavigationState, currentUser: AppUser, actions: AppNavigationActions) {
    val section = state.globalSection
    if (section == null) {
        EntryList(
            entries = state.visible.globalSections.map { it.label to { actions.onOpenGlobalSection(it) } },
        )
    } else {
        SectionHost(onBack = { actions.onOpenGlobalSection(null) }) {
            when (section) {
                GlobalSection.CIRCUITS -> CircuitRoute(currentUser)
                GlobalSection.CONGREGATIONS -> CongregationMetadataRoute(currentUser)
                GlobalSection.TALK_REFERENCE -> TalkReferenceRoute(currentUser)
            }
        }
    }
}

/** Übersicht des Hauptbereichs „Versammlung“, gruppiert in Verwaltung und Planung. */
@Composable
private fun CongregationOverview(sections: List<CongregationSection>, onOpen: (CongregationSection) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        CongregationGroup.entries.forEach { group ->
            val inGroup = sections.filter { it.group == group }
            if (inGroup.isNotEmpty()) {
                Text(
                    group.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
                inGroup.forEach { section ->
                    Text(
                        section.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(section) }
                            .padding(start = if (section.nested) 24.dp else 0.dp, top = 12.dp, bottom = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryList(entries: List<Pair<String, () -> Unit>>) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        entries.forEach { (label, onClick) ->
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
            )
        }
    }
}

/** Zeigt einen Unterbereich mit Rücksprung zur Übersicht; die Hauptleiste bleibt sichtbar. */
@Composable
private fun SectionHost(onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TextButton(onClick = onBack) { Text("Zurück zur Übersicht") }
        content()
    }
}

private val previewUser = AppUser(
    uid = "preview",
    approved = true,
    roles = setOf(CongregationRole.CONGREGATION_ADMIN, CongregationRole.TALK_PLANNER),
    isGlobalAdmin = true,
)

private val previewState = AppNavigationState(
    visible = VisibleNavigation(CongregationSection.entries, GlobalSection.entries),
    destination = TopLevelDestination.CONGREGATION,
)

@Preview(name = "Handy", widthDp = 360, heightDp = 720)
@Composable
fun AppScaffoldPhonePreview() {
    AppTheme { AppScaffold(previewState, "Firebase verfügbar", previewUser, AppNavigationActions()) }
}

@Preview(name = "Tablet", widthDp = 1000, heightDp = 700)
@Composable
fun AppScaffoldTabletPreview() {
    AppTheme { AppScaffold(previewState, "Firebase verfügbar", previewUser, AppNavigationActions()) }
}
