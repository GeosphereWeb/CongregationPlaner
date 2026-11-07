package de.geosphere.congregationplaner.user.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightCardColors
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.Appointment
import de.geosphere.congregationplaner.user.domain.model.MemberCategory
import de.geosphere.congregationplaner.user.domain.model.MemberProfile

@Composable
internal fun MemberEditor(
    editor: MemberEditorState,
    isSaving: Boolean,
    isDirty: Boolean,
    onChange: (MemberProfile) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val profile = editor.profile
    Card(colors = nightCardColors()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                editor.member?.let { "Angaben von ${it.label}" } ?: "Neue Person ohne Konto",
                style = MaterialTheme.typography.titleMedium,
            )
            ProfileField(profile.firstName, "Vorname", !isSaving, MemberProfile.MAX_NAME_LENGTH, {
                onChange(profile.copy(firstName = it))
            })
            ProfileField(profile.lastName, "Familienname", !isSaving, MemberProfile.MAX_NAME_LENGTH, {
                onChange(profile.copy(lastName = it))
            })

            ChipGroup("Gruppe") {
                MemberCategory.entries.forEach { category ->
                    FilterChip(
                        selected = profile.category == category,
                        onClick = { onChange(profile.copy(category = category)) },
                        label = { Text(category.label) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Getauft", modifier = Modifier.weight(1f))
                Switch(
                    checked = profile.baptized,
                    onCheckedChange = { onChange(profile.copy(baptized = it)) },
                    enabled = profile.category != MemberCategory.CHILD,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Redner", modifier = Modifier.weight(1f))
                Switch(
                    checked = profile.isSpeaker,
                    onCheckedChange = { onChange(profile.copy(isSpeaker = it)) },
                    enabled = profile.canBeSpeaker,
                )
            }
            if (profile.canHaveAppointment) {
                ChipGroup("Dienstamt") {
                    Appointment.entries.forEach { appointment ->
                        FilterChip(
                            selected = profile.appointment == appointment,
                            onClick = { onChange(profile.copy(appointment = appointment)) },
                            label = { Text(appointment.label) },
                        )
                    }
                }
            }

            ContactFields(profile, enabled = !isSaving, onChange = onChange)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, enabled = !isSaving && isDirty) {
                    Text(if (isSaving) "Speichert …" else "Speichern")
                }
                TextButton(onClick = onCancel, enabled = !isSaving) {
                    Text("Abbrechen")
                }
            }
        }
    }
}

@Composable
private fun ChipGroup(label: String, chips: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
    }
}

@ThemePreviews
@Composable
private fun MemberEditorPreview() = PreviewThemeWrapper {
    MemberEditor(
        editor = MemberEditorState(
            profile = MemberProfile(
                lastName = "Muster",
                firstName = "Max",
                category = MemberCategory.BROTHER,
                baptized = true,
                appointment = Appointment.ELDER,
                mobilePhone = "+49 170 1234567",
            ),
            member = AppUser(uid = "1", email = "max@example.com"),
        ),
        isSaving = false,
        isDirty = true,
        onChange = {},
        onSave = {},
        onCancel = {},
    )
}
