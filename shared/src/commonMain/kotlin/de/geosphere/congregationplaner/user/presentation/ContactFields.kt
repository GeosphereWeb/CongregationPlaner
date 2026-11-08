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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.button.SecondaryButton
import de.geosphere.congregationplaner.user.domain.model.EmergencyContact
import de.geosphere.congregationplaner.user.domain.model.MemberProfile

/** Das erste E-Mail-Feld ist immer da, auch ohne gespeicherte Adresse; weitere kommen bei Bedarf dazu. */
internal val MemberProfile.emailFields: List<String> get() = emails.ifEmpty { listOf("") }

internal val MemberProfile.canAddEmail: Boolean get() = emailFields.size < MemberProfile.MAX_EMAILS

internal val MemberProfile.canAddContact: Boolean
    get() = emergencyContacts.size < MemberProfile.MAX_EMERGENCY_CONTACTS

/** Bleibt nur ein leeres erstes Feld übrig, gibt es keine Adresse; so ändert sich das Profil nicht unnötig. */
internal fun MemberProfile.withEmail(index: Int, value: String): MemberProfile =
    copy(emails = emailFields.replaced(index, value).takeUnless { it == listOf("") }.orEmpty())

internal fun MemberProfile.withAddedEmail(): MemberProfile = if (canAddEmail) copy(emails = emailFields + "") else this

/** Das erste Feld bleibt immer; entfernt werden nur weitere. */
internal fun MemberProfile.withoutEmail(index: Int): MemberProfile =
    if (index == 0) this else copy(emails = emails.filterIndexed { i, _ -> i != index })

internal fun MemberProfile.withContact(index: Int, contact: EmergencyContact): MemberProfile =
    copy(emergencyContacts = emergencyContacts.replaced(index, contact))

internal fun MemberProfile.withAddedContact(): MemberProfile =
    if (canAddContact) copy(emergencyContacts = emergencyContacts + EmergencyContact()) else this

internal fun MemberProfile.withoutContact(index: Int): MemberProfile =
    copy(emergencyContacts = emergencyContacts.filterIndexed { i, _ -> i != index })

private fun <T> List<T>.replaced(index: Int, value: T): List<T> = mapIndexed { i, old ->
    if (i ==
        index
    ) {
        value
    } else {
        old
    }
}

/**
 * Telefon, E-Mail-Adressen, Adresse und Notfallkontakte eines Mitglieds; gemeinsam für „Mein Profil“ und den Editor
 * der Versammlungsadmins. Die Angaben sehen das Mitglied, die Admins und die Planer der Versammlung (ADR-0016).
 */
@Composable
internal fun ContactFields(
    profile: MemberProfile,
    enabled: Boolean,
    onChange: (MemberProfile) -> Unit,
    showAddressAndEmergencyContacts: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PhoneField(profile.mobilePhone, "Handy (optional)", smartphoneIcon, enabled, {
            onChange(profile.copy(mobilePhone = it))
        })
        PhoneField(profile.landlinePhone, "Festnetz (optional)", callIcon, enabled, {
            onChange(profile.copy(landlinePhone = it))
        })

        SectionTitle("E-Mail-Adressen", "Zusätzlich zur Anmelde-E-Mail.")
        profile.emailFields.forEachIndexed { index, email ->
            OutlinedTextField(
                value = email,
                onValueChange = { onChange(profile.withEmail(index, it.take(MemberProfile.MAX_EMAIL_LENGTH))) },
                label = { Text("E-Mail ${index + 1}") },
                leadingIcon = { Icon(mailIcon, contentDescription = null, tint = NightColors.OnBackgroundMuted) },
                // Das erste Feld bleibt immer; nur die weiteren lassen sich entfernen.
                trailingIcon = if (index > 0) {
                    {
                        IconButton(onClick = { onChange(profile.withoutEmail(index)) }, enabled = enabled) {
                            Icon(closeIcon, contentDescription = "E-Mail ${index + 1} entfernen")
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (profile.canAddEmail) {
            SecondaryButton(
                text = "E-Mail-Adresse hinzufügen",
                onClick = { onChange(profile.withAddedEmail()) },
                enabled = enabled,
                outlined = false,
            )
        }

        if (showAddressAndEmergencyContacts) {
            SectionTitle("Adresse", "Sehen die Admins und Planer der Versammlung.")
            ProfileField(profile.street, "Straße und Hausnummer", enabled, MemberProfile.MAX_STREET_LENGTH, {
                onChange(profile.copy(street = it))
            })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileField(
                    profile.postalCode,
                    "PLZ",
                    enabled,
                    MemberProfile.MAX_POSTAL_CODE_LENGTH,
                    { onChange(profile.copy(postalCode = it)) },
                    KeyboardType.Number,
                    Modifier.weight(1f),
                )
                ProfileField(
                    profile.city,
                    "Ort",
                    enabled,
                    MemberProfile.MAX_CITY_LENGTH,
                    { onChange(profile.copy(city = it)) },
                    modifier = Modifier.weight(2f),
                )
            }

            SectionTitle("Notfallkontakte", "Bis zu ${MemberProfile.MAX_EMERGENCY_CONTACTS}.")
            profile.emergencyContacts.forEachIndexed { index, contact ->
                EmergencyContactCard(
                    contact = contact,
                    number = index + 1,
                    enabled = enabled,
                    onChange = { onChange(profile.withContact(index, it)) },
                    onRemove = { onChange(profile.withoutContact(index)) },
                )
            }
            if (profile.canAddContact) {
                SecondaryButton(
                    text = "Notfallkontakt hinzufügen",
                    onClick = { onChange(profile.withAddedContact()) },
                    enabled = enabled,
                    height = 44.dp,
                )
            }
        }
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
        ProfileField(contact.name, "Name", enabled, MemberProfile.MAX_CONTACT_NAME_LENGTH, {
            onChange(contact.copy(name = it))
        })
        ProfileField(contact.relation, "Beziehung (optional)", enabled, MemberProfile.MAX_RELATION_LENGTH, {
            onChange(contact.copy(relation = it))
        })
        contact.phones.forEachIndexed { index, phone ->
            PhoneField(
                value = phone,
                label = "Telefon ${index + 1}",
                icon = phoneKindIcon(phone),
                enabled = enabled,
                onValueChange = { value -> onChange(contact.copy(phones = contact.phones.replaced(index, value))) },
                onRemove = if (contact.phones.size > 1) {
                    { onChange(contact.copy(phones = contact.phones.filterIndexed { i, _ -> i != index })) }
                } else {
                    null
                },
            )
        }
        if (contact.phones.size < MemberProfile.MAX_PHONES_PER_CONTACT) {
            SecondaryButton(
                text = "Telefonnummer hinzufügen",
                onClick = { onChange(contact.copy(phones = contact.phones + "")) },
                enabled = enabled,
                outlined = false,
            )
        }
        // Nur zum Lesen: der Ortsnetzname zur ersten Festnetznummer, nicht gespeichert.
        OutlinedTextField(
            value = contact.areaName.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Ort (aus der Festnetznummer)") },
            supportingText = if (contact.areaName == null) {
                { Text("Erscheint, sobald eine Festnetznummer eingetragen ist.") }
            } else {
                null
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
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
    onRemove: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(MemberProfile.MAX_PHONE_LENGTH)) },
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = NightColors.OnBackgroundMuted) },
        trailingIcon = onRemove?.let { remove ->
            {
                IconButton(onClick = remove, enabled = enabled) {
                    Icon(closeIcon, contentDescription = "$label entfernen", tint = NightColors.OnBackgroundMuted)
                }
            }
        },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        visualTransformation = PhoneVisualTransformation,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
@Suppress("LongParameterList")
internal fun ProfileField(
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

@ThemePreviews
@Composable
private fun ContactFieldsPreview() = PreviewThemeWrapper {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ContactFields(
            profile = MemberProfile(
                mobilePhone = "0171 1234567",
                emails = listOf("max@example.org", "max@work.example"),
                street = "Hauptstraße 1",
                postalCode = "12345",
                city = "Musterstadt",
                emergencyContacts = listOf(EmergencyContact("Erika Muster", "Ehefrau", listOf("0171 7654321"))),
            ),
            enabled = true,
            onChange = {},
        )
        ContactFields(profile = MemberProfile(), enabled = false, onChange = {
        }, showAddressAndEmergencyContacts = false)
    }
}
