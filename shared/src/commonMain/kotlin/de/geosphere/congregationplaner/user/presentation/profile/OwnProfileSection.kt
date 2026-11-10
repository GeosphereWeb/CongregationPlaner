package de.geosphere.congregationplaner.user.presentation.profile

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
import de.geosphere.congregationplaner.user.presentation.contact.ContactFields
import de.geosphere.congregationplaner.user.presentation.contact.ProfileField
import dev.zacsweers.metro.createGraph

/**
 * „Mein Profil“ unter „Einstellungen“: Namen, Telefon, E-Mail, Adresse und Notfallkontakte selbst pflegen. Nur für
 * Mitglieder mit eigenem Mitgliedsdokument und globale Admins, die ihre Angaben in ihrem Admin-Dokument pflegen
 * (ohne Versammlung, auch wenn lokal eine gemerkt ist). Globale Admins pflegen weder Adresse noch Notfallkontakte.
 */
@Composable
internal fun OwnProfileSection(user: AppUser, modifier: Modifier = Modifier) {
    if (!user.hasAccount || (user.congregation == null && !user.isGlobalAdmin)) return
    val viewModel = remember { createGraph<OwnProfileGraph>().ownProfileViewModel }
    LaunchedEffect(viewModel, user) { viewModel.onUserChanged(user) }
    val state by viewModel.uiState.collectAsState()
    OwnProfileForm(state, viewModel, showAddressAndEmergencyContacts = !user.isGlobalAdmin, modifier)
}

@Composable
private fun OwnProfileForm(
    state: OwnProfileUiState,
    viewModel: OwnProfileViewModel,
    showAddressAndEmergencyContacts: Boolean,
    modifier: Modifier = Modifier,
) {
    val form = state.form
    val enabled = !state.isSaving
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Mein Profil", style = MaterialTheme.typography.titleMedium)
        ProfileField(form.firstName, "Vorname", enabled, {
            viewModel.onFormChange(form.copy(firstName = it))
        })
        ProfileField(form.lastName, "Nachname", enabled, {
            viewModel.onFormChange(form.copy(lastName = it))
        })
        ContactFields(form, enabled, viewModel::onFormChange, showAddressAndEmergencyContacts)

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
