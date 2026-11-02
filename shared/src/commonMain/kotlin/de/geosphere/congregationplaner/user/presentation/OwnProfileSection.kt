package de.geosphere.congregationplaner.user.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.ui.button.PrimaryButton
import de.geosphere.congregationplaner.user.di.OwnProfileGraph
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import dev.zacsweers.metro.createGraph

/**
 * „Mein Profil“ unter „Einstellungen“: Vor- und Nachname selbst pflegen. Nur für Mitglieder mit eigenem
 * Mitgliedsdokument; globale Admins ohne Versammlung haben keines.
 */
@Composable
internal fun OwnProfileSection(user: AppUser, modifier: Modifier = Modifier) {
    if (user.congregation == null || !user.hasAccount) return
    val viewModel = remember { createGraph<OwnProfileGraph>().ownProfileViewModel }
    LaunchedEffect(viewModel, user) { viewModel.onUserChanged(user) }
    val state by viewModel.uiState.collectAsState()
    OwnProfileForm(
        state = state,
        onLastNameChange = viewModel::onLastNameChange,
        onFirstNameChange = viewModel::onFirstNameChange,
        onSave = viewModel::save,
        modifier = modifier,
    )
}

@Composable
private fun OwnProfileForm(
    state: OwnProfileUiState,
    onLastNameChange: (String) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Mein Profil", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.firstName,
            onValueChange = { onFirstNameChange(it.take(MemberProfile.MAX_NAME_LENGTH)) },
            label = { Text("Vorname") },
            singleLine = true,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.lastName,
            onValueChange = { onLastNameChange(it.take(MemberProfile.MAX_NAME_LENGTH)) },
            label = { Text("Nachname") },
            singleLine = true,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        state.message?.let { message ->
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.isError) MaterialTheme.colorScheme.error else NightColors.OnBackgroundMuted,
            )
        }
        PrimaryButton(
            text = if (state.isSaving) "Speichert …" else "Speichern",
            onClick = onSave,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
