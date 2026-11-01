package de.geosphere.congregationplaner.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.home.domain.model.HomeSummary
import de.geosphere.congregationplaner.home.domain.model.TalkSummary
import de.geosphere.congregationplaner.organization.domain.model.SelectedCongregation
import de.geosphere.congregationplaner.presentation.navigation.AppNavigationActions
import de.geosphere.congregationplaner.presentation.navigation.CongregationSection
import de.geosphere.congregationplaner.presentation.navigation.GlobalSection
import de.geosphere.congregationplaner.presentation.navigation.VisibleNavigation
import de.geosphere.congregationplaner.presentation.navigation.homeHeader
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.theming.AppTheme
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.ui.chip.StatusTone
import de.geosphere.congregationplaner.ui.header.NightPanel
import de.geosphere.congregationplaner.ui.header.TeleprompterHeader
import de.geosphere.congregationplaner.ui.nav.NavCardRow
import de.geosphere.congregationplaner.ui.nav.SectionLabel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Die Startseite: Kopfbereich und darunter je Rolle ein Block mit Karten, die den passenden Bereich öffnen.
 * Ohne Rolle erscheint ein Hinweis. Die Blöcke für Daten stehen in [state], die Kurzlinks ergeben sich aus [visible].
 */
@Composable
@Suppress("LongParameterList")
internal fun HomeContent(
    firebaseStatus: String,
    congregation: SelectedCongregation?,
    state: HomeUiState,
    visible: VisibleNavigation,
    actions: AppNavigationActions,
    today: LocalDate = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) {
    val header = remember(congregation, today) { homeHeader(congregation, today) }
    val summary = state.summary
    val hasRole = visible.congregationSections.isNotEmpty() || visible.globalSections.isNotEmpty()
    Column(Modifier.fillMaxSize()) {
        TeleprompterHeader(header.stepLabel, header.current, previous = header.previous, compact = true)
        NightPanel(Modifier.weight(1f)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                summary.talk?.let { TalkBlock(it, actions) }
                summary.pendingApprovals?.let { ApprovalsBlock(it, actions) }
                if (CongregationSection.MIDWEEK_PLANNING in visible.congregationSections) {
                    MidweekBlock(actions)
                }
                if (visible.globalSections.isNotEmpty()) GlobalBlock(visible.globalSections, actions)
                if (!hasRole) {
                    Text(
                        "Hier erscheinen deine Aufgaben, sobald dir eine Rolle zugewiesen ist.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = NightColors.OnBackgroundMuted,
                    )
                }
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text(
                    firebaseStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = NightColors.OnBackgroundMuted,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun TalkBlock(talk: TalkSummary, actions: AppNavigationActions) {
    val open = { actions.onOpenCongregationSection(CongregationSection.WEEKEND_TALKS) }
    SectionLabel("Als Vortragsplaner", Modifier.padding(top = 8.dp), StatusTone.Mint)
    val next = talk.nextSlot
    if (next != null) {
        NavCardRow(
            title = nextTalkTitle(next),
            onClick = open,
            supportingText = nextTalkDetails(next),
            badge = "Nächster",
            badgeTone = StatusTone.Gold,
        )
    } else {
        NavCardRow(NO_NEXT_TALK_TITLE, open, supportingText = NO_NEXT_TALK_DETAILS)
    }
    val none = talk.openDates.isEmpty()
    NavCardRow(
        title = openWeekendsTitle(talk.openDates.size),
        onClick = open,
        supportingText = openWeekendsDetails(talk.openDates),
        indicator = if (none) StatusTone.Mint else StatusTone.Coral,
    )
}

@Composable
private fun ApprovalsBlock(pending: Int, actions: AppNavigationActions) {
    SectionLabel("Als Versammlungsadmin", Modifier.padding(top = 8.dp), StatusTone.Gold)
    NavCardRow(
        title = approvalsTitle(pending),
        onClick = { actions.onOpenCongregationSection(CongregationSection.USER_MANAGEMENT) },
        indicator = if (pending > 0) StatusTone.Gold else null,
    )
}

@Composable
private fun MidweekBlock(actions: AppNavigationActions) {
    SectionLabel("Als Aufgabenplaner", Modifier.padding(top = 8.dp), StatusTone.Sky)
    NavCardRow(
        title = CongregationSection.MIDWEEK_PLANNING.label,
        onClick = { actions.onOpenCongregationSection(CongregationSection.MIDWEEK_PLANNING) },
    )
}

@Composable
private fun GlobalBlock(sections: List<GlobalSection>, actions: AppNavigationActions) {
    SectionLabel("Globale Verwaltung", Modifier.padding(top = 8.dp), StatusTone.Gold)
    sections.forEach { section ->
        NavCardRow(title = section.label, onClick = { actions.onOpenGlobalSection(section) })
    }
}

private val previewToday = LocalDate(2026, 11, 1)

private val previewNext = TalkSlot(
    LocalDate(2026, 11, 8),
    LocalTime(17, 30),
    talkNumber = 12,
    talkTitle = "Der Weg zum Glück",
    speakerMemberId = "m1",
    speakerLastName = "Muster",
    speakerFirstName = "Max",
)

private val previewState = HomeUiState(
    HomeSummary(
        talk = TalkSummary(previewNext, listOf(LocalDate(2026, 11, 15), LocalDate(2026, 11, 22))),
        pendingApprovals = 2,
    ),
)

@Composable
private fun HomePreview(state: HomeUiState, visible: VisibleNavigation) = AppTheme {
    HomeContent("Firebase verfügbar", null, state, visible, AppNavigationActions(), previewToday)
}

@Preview(name = "Vortragsplaner und Admin", widthDp = 360, heightDp = 780)
@Composable
private fun HomeTalkPlannerAndAdminPreview() = HomePreview(
    previewState,
    VisibleNavigation(CongregationSection.entries, emptyList()),
)

@Preview(name = "Globaler Admin", widthDp = 360, heightDp = 780)
@Composable
private fun HomeGlobalAdminPreview() = HomePreview(HomeUiState(), VisibleNavigation(emptyList(), GlobalSection.entries))

@Preview(name = "Ohne Rolle", widthDp = 360, heightDp = 780)
@Composable
private fun HomeWithoutRolePreview() = HomePreview(HomeUiState(), VisibleNavigation())
