package de.geosphere.congregationplaner.user.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserPermission
import de.geosphere.congregationplaner.user.domain.model.UserRole

@Composable
fun UserManagementScreen(viewModel: UserManagementViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    UserManagementContent(
        state = state,
        modifier = modifier,
        canEdit = viewModel::canEdit,
        onApprovedChange = viewModel::setApproved,
        onRoleChange = viewModel::setRole,
        onTogglePermission = viewModel::togglePermission,
        onRemove = viewModel::removeMember,
        onDismissError = viewModel::dismissError,
    )
}

@Composable
@Suppress("LongParameterList")
internal fun UserManagementContent(
    state: UserManagementUiState,
    canEdit: (AppUser) -> Boolean,
    onApprovedChange: (AppUser, Boolean) -> Unit,
    onRoleChange: (AppUser, UserRole) -> Unit,
    onTogglePermission: (AppUser, UserPermission) -> Unit,
    onRemove: (AppUser) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Userverwaltung", style = MaterialTheme.typography.headlineSmall)
        if (!state.canManage) {
            Text("Dafür fehlt dir die Berechtigung.")
            return@Column
        }

        state.inviteLink?.let { link ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Einladungslink", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Neue Nutzer können diesen Link im Login einfügen, um deine Versammlung auszuwählen.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    SelectionContainer { Text(link, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }

        state.errorMessage?.let { message ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                TextButton(onClick = onDismissError) { Text("OK") }
            }
        }

        if (state.isLoading) {
            CircularProgressIndicator()
            return@Column
        }
        if (state.members.isEmpty()) {
            Text("Noch keine Mitglieder.")
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.members, key = { it.uid }) { member ->
                MemberCard(
                    member = member,
                    isCurrentUser = member.uid == state.currentUserId,
                    enabled = canEdit(member) && member.uid !in state.busyMemberIds,
                    onApprovedChange = { onApprovedChange(member, it) },
                    onRoleChange = { onRoleChange(member, it) },
                    onTogglePermission = { onTogglePermission(member, it) },
                    onRemove = { onRemove(member) },
                )
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun MemberCard(
    member: AppUser,
    isCurrentUser: Boolean,
    enabled: Boolean,
    onApprovedChange: (Boolean) -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onTogglePermission: (UserPermission) -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (member.approved) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.secondaryContainer
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
                Text(if (member.approved) "Freigegeben" else "Wartet auf Freigabe")
                Switch(checked = member.approved, onCheckedChange = onApprovedChange, enabled = enabled)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Rolle:", style = MaterialTheme.typography.labelLarge)
                UserRole.assignable.forEach { role ->
                    FilterChip(
                        selected = member.role == role,
                        onClick = { if (member.role != role) onRoleChange(role) },
                        label = { Text(role.label) },
                        enabled = enabled,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Berechtigungen:", style = MaterialTheme.typography.labelLarge)
                UserPermission.entries.forEach { permission ->
                    FilterChip(
                        selected = permission in member.permissions,
                        onClick = { onTogglePermission(permission) },
                        label = { Text(permission.label) },
                        enabled = enabled,
                    )
                }
            }
            if (enabled) {
                TextButton(onClick = onRemove) {
                    Text("Aus der Versammlung entfernen", color = MaterialTheme.colorScheme.error)
                }
            }
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
                AppUser(uid = "2", displayName = "Admin", approved = true, role = UserRole.CONGREGATION_ADMIN),
            ),
            currentUserId = "2",
            inviteLink = "congregationplaner://join/region/EMEA/subregion/europe/branch/central-europe/" +
                "circuit/609a/congregation/berlin-mitte",
            isLoading = false,
            canManage = true,
        ),
        canEdit = { it.uid != "2" },
        onApprovedChange = { _, _ -> },
        onRoleChange = { _, _ -> },
        onTogglePermission = { _, _ -> },
        onRemove = {},
        onDismissError = {},
    )
}
