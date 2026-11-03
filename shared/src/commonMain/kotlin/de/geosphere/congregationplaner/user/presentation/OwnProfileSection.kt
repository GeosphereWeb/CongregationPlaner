package de.geosphere.congregationplaner.user.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.ui.button.PrimaryButton
import de.geosphere.congregationplaner.ui.button.SecondaryButton
import de.geosphere.congregationplaner.user.di.OwnProfileGraph
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.EmergencyContact
import de.geosphere.congregationplaner.user.domain.model.MemberPrivateDetails
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import dev.zacsweers.metro.createGraph

/**
 * „Mein Profil“ unter „Einstellungen“: Namen, Telefon, Adresse und Notfallkontakte selbst pflegen. Nur für Mitglieder
 * mit eigenem Mitgliedsdokument; globale Admins ohne Versammlung haben keines. Adresse und Notfallkontakte sehen nur
 * das Mitglied und die Versammlungsadmins (ADR-0015).
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
        ProfileField(form.firstName, "Vorname", enabled, MemberProfile.MAX_NAME_LENGTH, viewModel::onFirstNameChange)
        ProfileField(form.lastName, "Nachname", enabled, MemberProfile.MAX_NAME_LENGTH, viewModel::onLastNameChange)
        ProfileField(
            form.mobilePhone,
            "Handy (optional)",
            enabled,
            MemberProfile.MAX_PHONE_LENGTH,
            viewModel::onMobilePhoneChange,
            KeyboardType.Phone,
        )
        ProfileField(
            form.landlinePhone,
            "Festnetz (optional)",
            enabled,
            MemberProfile.MAX_PHONE_LENGTH,
            viewModel::onLandlinePhoneChange,
            KeyboardType.Phone,
        )

        SectionTitle("Adresse", "Sehen nur du und die Versammlungsadmins.")
        ProfileField(
            form.details.street,
            "Straße und Hausnummer",
            enabled,
            MemberPrivateDetails.MAX_STREET_LENGTH,
            viewModel::onStreetChange,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileField(
                form.details.postalCode,
                "PLZ",
                enabled,
                MemberPrivateDetails.MAX_POSTAL_CODE_LENGTH,
                viewModel::onPostalCodeChange,
                KeyboardType.Number,
                Modifier.weight(1f),
            )
            ProfileField(
                form.details.city,
                "Ort",
                enabled,
                MemberPrivateDetails.MAX_CITY_LENGTH,
                viewModel::onCityChange,
                modifier = Modifier.weight(2f),
            )
        }

        SectionTitle(
            "Notfallkontakte",
            "Bis zu ${MemberPrivateDetails.MAX_EMERGENCY_CONTACTS}; sehen nur du und die Versammlungsadmins.",
        )
        form.details.emergencyContacts.forEachIndexed { index, contact ->
            EmergencyContactCard(
                contact = contact,
                number = index + 1,
                enabled = enabled,
                onChange = { viewModel.onContactChange(index, it) },
                onRemove = { viewModel.removeContact(index) },
            )
        }
        if (state.canAddContact) {
            SecondaryButton(
                text = "Notfallkontakt hinzufügen",
                onClick = viewModel::addContact,
                enabled = enabled,
                height = 44.dp,
            )
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
            onClick = viewModel::save,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SectionTitle(title: String, hint: String) {
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = NightColors.OnBackground)
        Text(hint, style = MaterialTheme.typography.bodySmall, color = NightColors.OnBackgroundMuted)
    }
}

@Composable
private fun EmergencyContactCard(
    contact: EmergencyContact,
    number: Int,
    enabled: Boolean,
    onChange: (EmergencyContact) -> Unit,
    onRemove: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(NightColors.Surface, shape)
            .border(1.dp, NightColors.Border, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Kontakt $number",
                style = MaterialTheme.typography.labelLarge,
                color = NightColors.OnBackgroundBody,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = "Entfernen",
                onClick = onRemove,
                enabled = enabled,
                outlined = false,
                contentColor = NightColors.Coral,
            )
        }
        ProfileField(contact.name, "Name", enabled, MemberPrivateDetails.MAX_CONTACT_NAME_LENGTH, {
            onChange(contact.copy(name = it))
        })
        ProfileField(contact.relation, "Beziehung (optional)", enabled, MemberPrivateDetails.MAX_RELATION_LENGTH, {
            onChange(contact.copy(relation = it))
        })
        ProfileField(
            contact.phone,
            "Telefon",
            enabled,
            MemberProfile.MAX_PHONE_LENGTH,
            { onChange(contact.copy(phone = it)) },
            KeyboardType.Phone,
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun ProfileField(
    value: String,
    label: String,
    enabled: Boolean,
    maxLength: Int,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
    )
}
