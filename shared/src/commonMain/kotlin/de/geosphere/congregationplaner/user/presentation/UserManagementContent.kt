package de.geosphere.congregationplaner.user.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.Appointment
import de.geosphere.congregationplaner.user.domain.model.MemberCategory
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.model.UserPermission
import de.geosphere.congregationplaner.user.domain.model.UserRole
import kotlinx.coroutines.delay

/** Callbacks der Userverwaltung; gebündelt, damit die Composables überschaubare Signaturen behalten. */
internal data class UserManagementActions(
    val canEdit: (AppUser) -> Boolean = { false },
    val canEditProfile: (AppUser) -> Boolean = { false },
    val onApprovedChange: (AppUser, Boolean) -> Unit = { _, _ -> },
    val onRoleChange: (AppUser, UserRole) -> Unit = { _, _ -> },
    val onTogglePermission: (AppUser, UserPermission) -> Unit = { _, _ -> },
    val onRemove: (AppUser) -> Unit = {},
    val onEditProfile: (AppUser) -> Unit = {},
    val onAddMemberWithoutAccount: () -> Unit = {},
    val onEditorChange: (MemberProfile) -> Unit = {},
    val onSaveProfile: () -> Unit = {},
    val onCancelEditing: () -> Unit = {},
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
            onApprovedChange = viewModel::setApproved,
            onRoleChange = viewModel::setRole,
            onTogglePermission = viewModel::togglePermission,
            onRemove = viewModel::removeMember,
            onEditProfile = viewModel::editProfile,
            onAddMemberWithoutAccount = viewModel::addMemberWithoutAccount,
            onEditorChange = viewModel::updateEditor,
            onSaveProfile = viewModel::saveProfile,
            onCancelEditing = viewModel::cancelEditing,
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
    val navigator = rememberListDetailPaneScaffoldNavigator<Nothing>(
        initialDestinationHistory = listOf(ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List)),
    )
    val hasEditor = editor != null
    LaunchedEffect(hasEditor) {
        if (hasEditor) {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
        } else if (navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail) {
            navigator.navigateBack()
        }
    }
    // System-Zurück im Scaffold bedeutet: Bearbeitung abbrechen
    LaunchedEffect(navigator.currentDestination?.pane) {
        if (navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.List && hasEditor) {
            actions.onCancelEditing()
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Userverwaltung", style = MaterialTheme.typography.headlineSmall)
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

        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            scaffoldState = navigator.scaffoldState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            listPane = {
                AnimatedPane {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        state.inviteLink?.let { link -> item { InviteLinkCard(link) } }
                        item {
                            OutlinedButton(onClick = actions.onAddMemberWithoutAccount, enabled = editor == null) {
                                Text("Person ohne Konto hinzufügen")
                            }
                        }
                        if (state.members.isEmpty()) {
                            item { Text("Noch keine Mitglieder.") }
                        }
                        items(state.members, key = { it.uid }) { member ->
                            val isBusy = member.uid in state.busyMemberIds
                            MemberCard(
                                member = member,
                                isCurrentUser = member.uid == state.currentUserId,
                                enabled = actions.canEdit(member) && !isBusy,
                                canEditProfile = actions.canEditProfile(member) && !isBusy && editor == null,
                                actions = actions,
                            )
                        }
                    }
                }
            },
            detailPane = {
                AnimatedPane {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden) {
                            TextButton(onClick = actions.onCancelEditing, enabled = !state.isSaving) {
                                Text("Zurück zur Liste")
                            }
                        }
                        if (editor != null) {
                            MemberEditor(
                                editor = editor,
                                isSaving = state.isSaving,
                                onChange = actions.onEditorChange,
                                onSave = actions.onSaveProfile,
                                onCancel = actions.onCancelEditing,
                            )
                        } else {
                            Text("Wähle ein Mitglied zum Bearbeiten aus.")
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun InviteLinkCard(link: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Einladungslink", style = MaterialTheme.typography.titleSmall)
            Text(
                "Neue Nutzer können diesen Link im Login einfügen, um deine Versammlung auszuwählen.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SelectionContainer(modifier = Modifier.weight(1f)) {
                    Text(link, style = MaterialTheme.typography.bodyMedium)
                }
                CopyLinkButton(link)
            }
        }
    }
}

// Das plattformübergreifende Setzen von Text gibt es nur über den LocalClipboardManager; ClipEntry ist plattformspezifisch.
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
    Button(
        onClick = {
            clipboardManager.setText(AnnotatedString(link))
            copied = true
        },
    ) {
        Text(if (copied) "Kopiert" else "Link kopieren")
    }
}

private const val COPIED_FEEDBACK_MILLIS = 2_000L

@Composable
private fun MemberCard(
    member: AppUser,
    isCurrentUser: Boolean,
    enabled: Boolean,
    canEditProfile: Boolean,
    actions: UserManagementActions,
) {
    val isPending = member.hasAccount && !member.approved
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPending) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isCurrentUser) "${member.label} (du)" else member.label,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    member.email?.takeIf { it != member.label }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (member.hasAccount) {
                    Text(if (member.approved) "Freigegeben" else "Wartet auf Freigabe")
                    Switch(
                        checked = member.approved,
                        onCheckedChange = { actions.onApprovedChange(member, it) },
                        enabled = enabled,
                    )
                } else {
                    Text("Ohne Konto", style = MaterialTheme.typography.labelLarge)
                }
            }
            ProfileSummary(member.profile)
            if (member.hasAccount) {
                AccessSettings(member, enabled, actions)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canEditProfile) {
                    TextButton(onClick = { actions.onEditProfile(member) }) { Text("Angaben bearbeiten") }
                }
                if (enabled) {
                    TextButton(onClick = { actions.onRemove(member) }) {
                        Text("Aus der Versammlung entfernen", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSummary(profile: MemberProfile) {
    val details = listOfNotNull(
        profile.category?.label,
        "getauft".takeIf { profile.baptized },
        profile.appointment.takeIf { it != Appointment.NONE }?.label,
    )
    Text(
        details.joinToString(" · ").ifEmpty { "Noch keine Angaben" },
        style = MaterialTheme.typography.bodyMedium,
    )
    val phones = listOfNotNull(
        profile.mobilePhone.takeIf(String::isNotBlank)?.let { "Handy: $it" },
        profile.landlinePhone.takeIf(String::isNotBlank)?.let { "Festnetz: $it" },
    )
    if (phones.isNotEmpty()) {
        SelectionContainer { Text(phones.joinToString("   "), style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun AccessSettings(member: AppUser, enabled: Boolean, actions: UserManagementActions) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Rolle:",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        UserRole.assignable.forEach { role ->
            FilterChip(
                selected = member.role == role,
                onClick = { if (member.role != role) actions.onRoleChange(member, role) },
                label = { Text(role.label) },
                enabled = enabled,
            )
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Berechtigungen:",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        UserPermission.entries.forEach { permission ->
            FilterChip(
                selected = permission in member.permissions,
                onClick = { actions.onTogglePermission(member, permission) },
                label = { Text(permission.label) },
                enabled = enabled,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun UserManagementPreview() = PreviewThemeWrapper {
    UserManagementContent(
        state = UserManagementUiState(
            members = listOf(
                AppUser(uid = "1", email = "neu@congregationplaner.de", permissions = UserPermission.defaults),
                AppUser(
                    uid = "2",
                    displayName = "Admin",
                    approved = true,
                    role = UserRole.CONGREGATION_ADMIN,
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
        actions = UserManagementActions(canEdit = { it.uid != "2" }, canEditProfile = { true }),
    )
}
