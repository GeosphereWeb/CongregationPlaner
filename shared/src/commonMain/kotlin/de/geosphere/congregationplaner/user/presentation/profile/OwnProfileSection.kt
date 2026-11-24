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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.presentation.components.rememberScopedViewModel
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.button.PrimaryButton
import de.geosphere.congregationplaner.user.di.OwnProfileGraph
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.EmergencyContact
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.presentation.contact.ContactFields
import de.geosphere.congregationplaner.user.presentation.contact.EmergencyContactFields
import de.geosphere.congregationplaner.user.presentation.contact.ProfileField
import dev.zacsweers.metro.createGraph

/** Welcher Teil des eigenen Profils ein Reiter der Einstellungen zeigt. */
internal enum class OwnProfilePart { CONTACT, EMERGENCY_CONTACTS }

/**
 * Das ViewModel des eigenen Profils, einmal für beide Reiter, damit ungespeicherte Eingaben den Wechsel überstehen.
 */
@Composable
internal fun rememberOwnProfileViewModel(user: AppUser): OwnProfileViewModel {
    val viewModel = rememberScopedViewModel { createGraph<OwnProfileGraph>().ownProfileViewModel }
    LaunchedEffect(viewModel, user) { viewModel.onUserChanged(user) }
    return viewModel
}

/**
 * „Mein Profil“ bzw. „Notfallkontakte“ unter „Einstellungen“: Namen, Telefon, E-Mail und Adresse oder die
 * Notfallkontakte selbst pflegen; beide Teile speichern dasselbe Profil. Globale Admins pflegen keine Adresse.
 */
@Composable
internal fun OwnProfileSection(
    user: AppUser,
    part: OwnProfilePart,
    viewModel: OwnProfileViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    OwnProfileForm(
        state = state,
        actions = OwnProfileActions(onFormChange = viewModel::onFormChange, onSave = viewModel::save),
        part = part,
        showAddress = !user.isGlobalAdmin,
        modifier = modifier,
    )
}

/** Callbacks des eigenen Profils. */
internal data class OwnProfileActions(val onFormChange: (MemberProfile) -> Unit = {}, val onSave: () -> Unit = {})

@Composable
private fun OwnProfileForm(
    state: OwnProfileUiState,
    actions: OwnProfileActions,
    part: OwnProfilePart,
    showAddress: Boolean,
    modifier: Modifier = Modifier,
) {
    val form = state.form
    val enabled = !state.isSaving
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (part) {
            OwnProfilePart.CONTACT -> {
                ProfileField(form.firstName, "Vorname", enabled, {
                    actions.onFormChange(form.copy(firstName = it))
                })
                ProfileField(form.lastName, "Nachname", enabled, {
                    actions.onFormChange(form.copy(lastName = it))
                })
                ContactFields(form, enabled, actions.onFormChange, showAddress, showEmergencyContacts = false)
            }
            OwnProfilePart.EMERGENCY_CONTACTS -> EmergencyContactFields(form, enabled, actions.onFormChange)
        }

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
            part = OwnProfilePart.CONTACT,
            showAddress = true,
        )
        OwnProfileForm(
            state = OwnProfileUiState(form = saved, saved = saved, message = "Gespeichert."),
            actions = OwnProfileActions(),
            part = OwnProfilePart.CONTACT,
            showAddress = false,
        )
        OwnProfileForm(
            state = OwnProfileUiState(
                form = saved.copy(
                    emergencyContacts = listOf(EmergencyContact("Erika Muster", "", listOf("030 123456"))),
                ),
                saved = saved,
            ),
            actions = OwnProfileActions(),
            part = OwnProfilePart.EMERGENCY_CONTACTS,
            showAddress = true,
        )
    }
}
