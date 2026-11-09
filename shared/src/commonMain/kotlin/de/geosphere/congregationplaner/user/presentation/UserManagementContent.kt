package de.geosphere.congregationplaner.user.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.presentation.components.CollapsingHeader
import de.geosphere.congregationplaner.presentation.components.DetailPane
import de.geosphere.congregationplaner.presentation.components.DetailPlaceholder
import de.geosphere.congregationplaner.presentation.components.ListPaneHeader
import de.geosphere.congregationplaner.presentation.components.collapsingHeaderScroll
import de.geosphere.congregationplaner.presentation.components.rememberCollapsingHeaderState
import de.geosphere.congregationplaner.presentation.components.rememberListDetailNavigation
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.button.SecondaryButton
import de.geosphere.congregationplaner.ui.button.SpeedDialFab
import de.geosphere.congregationplaner.ui.button.SpeedDialItem
import de.geosphere.congregationplaner.ui.field.AppSwitch
import de.geosphere.congregationplaner.ui.nav.NavCardRow
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.Appointment
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.model.MemberCategory
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import kotlinx.coroutines.delay

/** Callbacks der Userverwaltung; gebündelt, damit die Composables überschaubare Signaturen behalten. */
internal data class UserManagementActions(
    val canEdit: (AppUser) -> Boolean = { false },
    val canEditProfile: (AppUser) -> Boolean = { false },
    val canToggleRole: (AppUser, CongregationRole) -> Boolean = { _, _ -> false },
    val adminRoleLockReason: (AppUser) -> String? = { null },
    val onApprovedChange: (AppUser, Boolean) -> Unit = { _, _ -> },
    val onToggleRole: (AppUser, CongregationRole) -> Unit = { _, _ -> },
    val onRemove: (AppUser) -> Unit = {},
    val onEditProfile: (AppUser) -> Unit = {},
    val onAddMemberWithoutAccount: () -> Unit = {},
    val onEditorChange: (MemberProfile) -> Unit = {},
    val onSaveProfile: () -> Unit = {},
    val onCancelEditing: () -> Unit = {},
    val onDiscardChanges: () -> Unit = {},
    val onDismissError: () -> Unit = {},
)

@Composable
fun UserManagementScreen(viewModel: UserManagementViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    UserManagementContent(
        state = state,
        modifier = modifier,
        actions = UserManagementActions(
            canEdit = viewModel::canEdit,
            canEditProfile = viewModel::canEditProfile,
            canToggleRole = viewModel::canToggleRole,
            adminRoleLockReason = viewModel::adminRoleLockReason,
            onApprovedChange = viewModel::setApproved,
            onToggleRole = viewModel::toggleRole,
            onRemove = viewModel::removeMember,
            onEditProfile = viewModel::editProfile,
            onAddMemberWithoutAccount = viewModel::addMemberWithoutAccount,
            onEditorChange = viewModel::updateEditor,
            onSaveProfile = viewModel::saveProfile,
            onCancelEditing = viewModel::cancelEditing,
            onDiscardChanges = viewModel::discardChanges,
            onDismissError = viewModel::dismissError,
        ),
    )
}

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Suppress("LongMethod")
internal fun UserManagementContent(
    state: UserManagementUiState,
    actions: UserManagementActions,
    modifier: Modifier = Modifier,
) {
    val editor = state.editor
    val navigator = rememberListDetailNavigation(state, onClose = actions.onCancelEditing)
    val singlePane = navigator.scaffoldDirective.maxHorizontalPartitions == 1
    val onCancelEditing = if (singlePane) actions.onCancelEditing else actions.onDiscardChanges
    val listState = rememberLazyListState()
    val header = rememberCollapsingHeaderState(listState)

    Column(
        modifier = modifier.fillMaxSize().collapsingHeaderScroll(header).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ListPaneHeader("Userverwaltung")
        if (!state.canManage) {
            Text("Dafür fehlt dir die Berechtigung.")
            return@Column
        }

        state.errorMessage?.let { message ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                TextButton(onClick = actions.onDismissError) { Text("OK") }
            }
        }

        if (state.isLoading) {
            CircularProgressIndicator()
            return@Column
        }

        state.inviteLink?.let { link -> CollapsingHeader(visible = header.visible) { InviteLinkCard(link) } }

        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            scaffoldState = navigator.scaffoldState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            listPane = {
                AnimatedPane {
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            // Platz unter dem letzten Mitglied, damit der FAB es nicht verdeckt.
                            contentPadding = PaddingValues(bottom = FAB_CLEARANCE),
                        ) {
                            if (state.members.isEmpty()) {
                                item { Text("Noch keine Mitglieder.") }
                            }
                            items(state.members, key = { it.uid }) { member ->
                                NavCardRow(
                                    title = memberTitle(member, member.uid == state.currentUserId),
                                    onClick = {
                                        if (actions.canEditProfile(member) && member.uid !in state.busyMemberIds) {
                                            actions.onEditProfile(member)
                                        }
                                    },
                                    supportingText = memberSubtitle(member),
                                    selected = editor?.member?.uid == member.uid,
                                    badge = memberBadge(member),
                                )
                            }
                        }
                        SpeedDialFab(
                            items = listOf(
                                SpeedDialItem("Person ohne Konto hinzufügen", actions.onAddMemberWithoutAccount),
                            ),
                            contentDescription = "Mitglieder hinzufügen",
                            enabled = !state.isSaving,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        )
                    }
                }
            },
            detailPane = {
                AnimatedPane {
                    DetailPane(
                        showBack = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden,
                        onBack = actions.onCancelEditing,
                        backEnabled = !state.isSaving,
                    ) {
                        if (editor != null) {
                            // Freigabe und Rollen ändern sich sofort; deshalb gilt der aktuelle Stand aus der Liste.
                            val member = editor.member?.let { selected ->
                                state.members.firstOrNull { it.uid == selected.uid } ?: selected
                            }
                            val isBusy = member?.uid in state.busyMemberIds
                            if (member != null && member.hasAccount) {
                                MemberAccessCard(member, isBusy, actions)
                            }
                            MemberEditor(
                                editor = editor,
                                isSaving = state.isSaving,
                                isDirty = editor.isDirty,
                                onChange = actions.onEditorChange,
                                onSave = actions.onSaveProfile,
                                onCancel = onCancelEditing,
                            )
                            if (member != null && actions.canEdit(member)) {
                                SecondaryButton(
                                    text = "Aus der Versammlung entfernen",
                                    onClick = { actions.onRemove(member) },
                                    enabled = !isBusy && !state.isSaving,
                                    outlined = false,
                                    contentColor = NightColors.Coral,
                                )
                            }
                        } else {
                            DetailPlaceholder("Wähle ein Mitglied zum Bearbeiten aus.")
                        }
                    }
                }
            },
        )
    }
}

/** Fläche mit Rand und Radius 8 wie die übrigen Karten; Grundlage für Einladung und Zugang. */
@Composable
private fun DesignCard(
    containerColor: Color = NightColors.Surface,
    borderColor: Color = NightColors.Border,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(containerColor, shape)
            .border(1.dp, borderColor, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun InviteLinkCard(link: String) {
    // Blau wie eine Info-Fläche, damit der Bereich nicht mit den Mitgliedern darunter verschwimmt.
    DesignCard(containerColor = NightColors.SkyContainer, borderColor = NightColors.SkyBorder) {
        Text("Einladungslink", style = MaterialTheme.typography.titleSmall, color = NightColors.Sky)
        Text(
            "Neue Nutzer können diesen Link im Login einfügen, um deine Versammlung auszuwählen.",
            style = MaterialTheme.typography.bodySmall,
            color = NightColors.OnBackgroundMuted,
        )
        // Der Link selbst bleibt unsichtbar; er wird nur über den Knopf in die Zwischenablage kopiert.
        CopyLinkButton(link)
    }
}

// Das plattformübergreifende Setzen von Text gibt es nur über den LocalClipboardManager; ClipEntry ist
// plattformspezifisch.
@Suppress("DEPRECATION")
@Composable
private fun CopyLinkButton(link: String) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember(link) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_FEEDBACK_MILLIS)
            copied = false
        }
    }
    SecondaryButton(
        text = if (copied) "Kopiert" else "Link kopieren",
        onClick = {
            clipboardManager.setText(AnnotatedString(link))
            copied = true
        },
        height = 44.dp,
    )
}

private const val COPIED_FEEDBACK_MILLIS = 2_000L

private val FAB_CLEARANCE = 88.dp

/** Freigabe und Rollen im Detail; beides wirkt sofort, ohne „Speichern“. */
@Composable
private fun MemberAccessCard(member: AppUser, isBusy: Boolean, actions: UserManagementActions) {
    DesignCard {
        Text("Zugang", style = MaterialTheme.typography.titleSmall, color = NightColors.OnBackground)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (member.approved) "Freigegeben" else "Wartet auf Freigabe",
                modifier = Modifier.weight(1f),
                color = NightColors.OnBackgroundBody,
            )
            AppSwitch(
                checked = member.approved,
                onCheckedChange = { actions.onApprovedChange(member, it) },
                enabled = actions.canEdit(member) && !isBusy,
            )
        }
        AccessSettings(member, isBusy, actions)
    }
}

@Composable
private fun AccessSettings(member: AppUser, isBusy: Boolean, actions: UserManagementActions) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Rollen:",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        CongregationRole.entries.forEach { role ->
            FilterChip(
                selected = role in member.roles,
                onClick = { actions.onToggleRole(member, role) },
                label = { Text(role.label) },
                enabled = actions.canToggleRole(member, role) && !isBusy,
            )
        }
    }
    actions.adminRoleLockReason(member)?.let { reason ->
        Text(reason, style = MaterialTheme.typography.bodySmall)
    }
}

@ThemePreviews
@Composable
private fun UserManagementPreview() = PreviewThemeWrapper {
    UserManagementContent(
        state = UserManagementUiState(
            members = listOf(
                AppUser(uid = "1", email = "neu@congregationplaner.de"),
                AppUser(
                    uid = "2",
                    displayName = "Admin",
                    approved = true,
                    roles = setOf(CongregationRole.CONGREGATION_ADMIN, CongregationRole.TALK_PLANNER),
                    profile = MemberProfile(
                        lastName = "Muster",
                        firstName = "Max",
                        category = MemberCategory.BROTHER,
                        baptized = true,
                        appointment = Appointment.ELDER,
                        mobilePhone = "+49 170 1234567",
                    ),
                ),
                AppUser(
                    uid = "3",
                    hasAccount = false,
                    profile = MemberProfile(lastName = "Muster", firstName = "Lena", category = MemberCategory.CHILD),
                ),
            ),
            currentUserId = "2",
            inviteLink = "congregationplaner://join/region/EMEA/subregion/europe/branch/central-europe/" +
                "circuit/609a/congregation/berlin-mitte",
            isLoading = false,
            canManage = true,
        ),
        actions = UserManagementActions(
            canEdit = { it.uid != "2" },
            canEditProfile = { true },
            canToggleRole = { _, _ -> true },
        ),
    )
}
