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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
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
        PhoneField(form.mobilePhone, "Handy (optional)", smartphoneIcon, enabled, viewModel::onMobilePhoneChange)
        PhoneField(form.landlinePhone, "Festnetz (optional)", callIcon, enabled, viewModel::onLandlinePhoneChange)

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
        PhoneField(contact.phone, "Telefon", callIcon, enabled, { onChange(contact.copy(phone = it)) })
    }
}

/** Telefonfeld mit Symbol vorn; beim Tippen schon so gruppiert, wie es gespeichert wird. */
@Composable
private fun PhoneField(
    value: String,
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(MemberProfile.MAX_PHONE_LENGTH)) },
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = NightColors.OnBackgroundMuted) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        visualTransformation = PhoneVisualTransformation,
        modifier = Modifier.fillMaxWidth(),
    )
}

// Smartphone und Telefonhörer aus den Material Icons (Apache 2.0).
private val smartphoneIcon = vectorIcon(
    "smartphone",
    "M17,1.01L7,1c-1.1,0 -2,0.9 -2,2v18c0,1.1 0.9,2 2,2h10c1.1,0 2,-0.9 2,-2V3c0,-1.1 -0.9,-1.99 -2,-1.99zM17,19H7V5h10v14z",
)

private val callIcon = vectorIcon(
    "call",
    "M6.62,10.79c1.44,2.83 3.76,5.14 6.59,6.59l2.2,-2.2c0.27,-0.27 0.67,-0.36 1.02,-0.24 1.12,0.37 2.33,0.57 " +
        "3.57,0.57 0.55,0 1,0.45 1,1V20c0,0.55 -0.45,1 -1,1 -9.39,0 -17,-7.61 -17,-17 0,-0.55 0.45,-1 1,-1h3.5" +
        "c0.55,0 1,0.45 1,1 0,1.25 0.2,2.45 0.57,3.57 0.11,0.35 0.03,0.74 -0.25,1.02l-2.2,2.2z",
)

private fun vectorIcon(name: String, path: String): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black)).build()

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
