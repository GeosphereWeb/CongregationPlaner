package de.geosphere.congregationplaner.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.circuit.presentation.CircuitRoute
import de.geosphere.congregationplaner.congregation.presentation.CongregationMetadataRoute
import de.geosphere.congregationplaner.talk.presentation.ExternalDirectoryRoute
import de.geosphere.congregationplaner.talk.presentation.TalkHistoryRoute
import de.geosphere.congregationplaner.talk.presentation.TalkReferenceRoute
import de.geosphere.congregationplaner.talk.presentation.TalkScheduleRoute
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.presentation.UserManagementRoute

private val secondColumnWidth = 280.dp

/** Ein Eintrag der Übersicht bzw. der zweiten Spalte; [group] gliedert die Liste, [nested] rückt den Eintrag ein. */
internal data class NavEntry(
    val label: String,
    val group: String? = null,
    val nested: Boolean = false,
    val selected: Boolean = false,
    val onClick: () -> Unit = {},
)

/**
 * Liste der Bereiche mit Gruppenüberschriften. Auf dem Handy ist sie die Übersicht, auf großen Fenstern die feste
 * zweite Spalte neben der Hauptleiste.
 */
@Composable
internal fun EntryList(entries: List<NavEntry>, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        var lastGroup: String? = null
        entries.forEach { entry ->
            if (entry.group != null && entry.group != lastGroup) {
                Text(
                    entry.group.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
            }
            lastGroup = entry.group
            Text(
                entry.label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (entry.selected) {
                            Modifier.background(
                                MaterialTheme.colorScheme.secondaryContainer,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .clickable(onClick = entry.onClick)
                    .padding(start = if (entry.nested) 24.dp else 8.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            )
        }
    }
}

/** Obere Leiste mit Zurück-Pfeil und, falls gewünscht, Tabs; die Hauptleiste bleibt sichtbar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionScreen(
    title: String,
    onBack: () -> Unit,
    tabs: List<Pair<String, Boolean>> = emptyList(),
    onSelectTab: (Int) -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(arrowBackIcon, contentDescription = "Zurück") }
            },
        )
        if (tabs.isNotEmpty()) {
            PrimaryTabRow(selectedTabIndex = tabs.indexOfFirst { it.second }.coerceAtLeast(0)) {
                tabs.forEachIndexed { index, (label, selected) ->
                    Tab(selected = selected, onClick = { onSelectTab(index) }, text = { Text(label) })
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
    }
}

/** Handy: Übersicht oder Unterbereich. Breit: feste zweite Spalte und rechts der Inhalt. */
@Composable
private fun AreaLayout(
    isWide: Boolean,
    entries: List<NavEntry>,
    selectedTitle: String?,
    onBack: () -> Unit,
    tabs: List<Pair<String, Boolean>> = emptyList(),
    onSelectTab: (Int) -> Unit = {},
    content: (@Composable () -> Unit)?,
) {
    when {
        isWide -> Row(Modifier.fillMaxSize()) {
            EntryList(entries, Modifier.width(secondColumnWidth).fillMaxHeight())
            VerticalDivider()
            Box(Modifier.weight(1f).fillMaxHeight()) {
                if (content != null) content() else Text("Wähle einen Bereich.", Modifier.padding(16.dp))
            }
        }

        content == null || selectedTitle == null -> EntryList(entries, Modifier.fillMaxSize())

        else -> SectionScreen(selectedTitle, onBack, tabs, onSelectTab, content)
    }
}

@Composable
internal fun CongregationArea(
    state: AppNavigationState,
    currentUser: AppUser,
    isWide: Boolean,
    actions: AppNavigationActions,
) {
    val selected = state.congregationSection
    val entries = state.visible.congregationSections.map { section ->
        NavEntry(
            label = section.label,
            group = section.group.label,
            nested = section.nested,
            selected = section == selected,
            onClick = { actions.onOpenCongregationSection(section) },
        )
    }
    // Die Vortragsplanung ist auf dem Handy ein Bereich mit Tabs, auf großen Fenstern stehen die Teile in der Spalte.
    val talkTabs = state.visible.congregationSections.filter { it in CongregationSection.talkPlanning }
    val inTalkPlanning = !isWide && selected != null && selected in talkTabs
    AreaLayout(
        isWide = isWide,
        entries = entries,
        selectedTitle = if (inTalkPlanning) "Vortragsplanung" else selected?.label,
        onBack = { actions.onOpenCongregationSection(null) },
        tabs = if (inTalkPlanning) talkTabs.map { it.tabLabel to (it == selected) } else emptyList(),
        onSelectTab = { index -> actions.onOpenCongregationSection(talkTabs[index]) },
        content = selected?.let { section -> { CongregationSectionContent(section, currentUser) } },
    )
}

@Composable
internal fun GlobalArea(
    state: AppNavigationState,
    currentUser: AppUser,
    isWide: Boolean,
    actions: AppNavigationActions,
) {
    val selected = state.globalSection
    val entries = state.visible.globalSections.map { section ->
        NavEntry(
            label = section.label,
            selected = section == selected,
            onClick = { actions.onOpenGlobalSection(section) },
        )
    }
    AreaLayout(
        isWide = isWide,
        entries = entries,
        selectedTitle = selected?.label,
        onBack = { actions.onOpenGlobalSection(null) },
        content = selected?.let { section -> { GlobalSectionContent(section, currentUser) } },
    )
}

@Composable
private fun CongregationSectionContent(section: CongregationSection, currentUser: AppUser) {
    when (section) {
        CongregationSection.GENERAL_DATA -> CongregationMetadataRoute(currentUser, ownOnly = true)
        CongregationSection.USER_MANAGEMENT -> UserManagementRoute(currentUser)
        CongregationSection.MIDWEEK_PLANNING ->
            Text("leben_und_dienst \n Schätze \n uns verbessern \n leben als christ", Modifier.padding(16.dp))
        CongregationSection.WEEKEND_TALKS -> TalkScheduleRoute(currentUser)
        CongregationSection.EXTERNAL_SPEAKERS -> ExternalDirectoryRoute(currentUser)
        CongregationSection.TALK_HISTORY -> TalkHistoryRoute(currentUser)
    }
}

@Composable
private fun GlobalSectionContent(section: GlobalSection, currentUser: AppUser) {
    when (section) {
        GlobalSection.CIRCUITS -> CircuitRoute(currentUser)
        GlobalSection.CONGREGATIONS -> CongregationMetadataRoute(currentUser)
        GlobalSection.TALK_REFERENCE -> TalkReferenceRoute(currentUser)
    }
}
