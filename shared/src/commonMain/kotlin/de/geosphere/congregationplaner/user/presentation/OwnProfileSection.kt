package de.geosphere.congregationplaner.user.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
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
 * „Mein Profil“ unter „Einstellungen“: Namen, Telefon, E-Mail, Adresse und Notfallkontakte selbst pflegen. Nur für
 * Mitglieder mit eigenem Mitgliedsdokument; globale Admins ohne Versammlung haben keines.
 */
@Composable
internal fun OwnProfileSection(user: AppUser, modifier: Modifier = Modifier) {
    if (user.congregation == null || !user.hasAccount) return
    val viewModel = remember { createGraph<OwnProfileGraph>().ownProfileViewModel }
    LaunchedEffect(viewModel, user) { viewModel.onUserChanged(user) }
    val state by viewModel.uiState.collectAsState()
    OwnProfileForm(state, viewModel, modifier)
}

@Composable
private fun OwnProfileForm(state: OwnProfileUiState, viewModel: OwnProfileViewModel, modifier: Modifier = Modifier) {
    val form = state.form
    val enabled = !state.isSaving
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Mein Profil", style = MaterialTheme.typography.titleMedium)
        ProfileField(form.firstName, "Vorname", enabled, MemberProfile.MAX_NAME_LENGTH, {
            viewModel.onFormChange(form.copy(firstName = it))
        })
        ProfileField(form.lastName, "Nachname", enabled, MemberProfile.MAX_NAME_LENGTH, {
            viewModel.onFormChange(form.copy(lastName = it))
        })
        ContactFields(form, enabled, viewModel::onFormChange)

        state.message?.let { message ->
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.isError) MaterialTheme.colorScheme.error else NightColors.OnBackgroundMuted,
            )
        }
        PrimaryButton(
            text = if (state.isSaving) "Speichert …" else "Speichern",
            onClick = viewModel::save,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
