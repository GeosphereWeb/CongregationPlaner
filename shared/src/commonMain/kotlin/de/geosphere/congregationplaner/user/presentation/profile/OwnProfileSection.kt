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
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.button.PrimaryButton
import de.geosphere.congregationplaner.user.di.OwnProfileGraph
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
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
    OwnProfileForm(
        state = state,
        actions = OwnProfileActions(onFormChange = viewModel::onFormChange, onSave = viewModel::save),
        showAddressAndEmergencyContacts = !user.isGlobalAdmin,
        modifier = modifier,
    )
}

/** Callbacks des eigenen Profils. */
internal data class OwnProfileActions(val onFormChange: (MemberProfile) -> Unit = {}, val onSave: () -> Unit = {})

@Composable
private fun OwnProfileForm(
    state: OwnProfileUiState,
    actions: OwnProfileActions,
    showAddressAndEmergencyContacts: Boolean,
    modifier: Modifier = Modifier,
) {
    val form = state.form
    val enabled = !state.isSaving
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Mein Profil", style = MaterialTheme.typography.titleMedium)
        ProfileField(form.firstName, "Vorname", enabled, {
            actions.onFormChange(form.copy(firstName = it))
        })
        ProfileField(form.lastName, "Nachname", enabled, {
            actions.onFormChange(form.copy(lastName = it))
        })
        ContactFields(form, enabled, actions.onFormChange, showAddressAndEmergencyContacts)

        state.message?.let { message ->
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.isError) MaterialTheme.colorScheme.error else NightColors.OnBackgroundMuted,
            )
        }
        PrimaryButton(
            text = if (state.isSaving) "Speichert …" else "Speichern",
            onClick = actions.onSave,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@ThemePreviews
@Composable
private fun OwnProfileFormPreview() = PreviewThemeWrapper {
    val saved = MemberProfile(lastName = "Muster", firstName = "Max", mobilePhone = "0170 1234567")
    Column {
        OwnProfileForm(
            state = OwnProfileUiState(form = saved.copy(city = "Berlin"), saved = saved),
            actions = OwnProfileActions(),
            showAddressAndEmergencyContacts = true,
        )
        OwnProfileForm(
            state = OwnProfileUiState(form = saved, saved = saved, message = "Gespeichert."),
            actions = OwnProfileActions(),
            showAddressAndEmergencyContacts = false,
        )
    }
}
