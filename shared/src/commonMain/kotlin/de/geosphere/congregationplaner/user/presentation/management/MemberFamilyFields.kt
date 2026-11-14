package de.geosphere.congregationplaner.user.presentation.management

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.FamilyRelation
import de.geosphere.congregationplaner.user.domain.model.MemberProfile

/**
 * Abschnitt „Familie“ im Mitglieder-Editor: Ein Oberhaupt sieht seine Familie, alle anderen wählen ein Oberhaupt und
 * ihre Beziehung zu ihm.
 */
@Composable
internal fun MemberFamilyFields(
    profile: MemberProfile,
    family: EditorFamily,
    enabled: Boolean,
    onChange: (MemberProfile) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Familie", style = MaterialTheme.typography.labelLarge)
        if (family.isHead) {
            Text("Familienoberhaupt von: ${family.relativesText}", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        FamilyHeadSelector(family, enabled) { onChange(profile.copy(familyHeadId = it)) }
        if (profile.familyHeadId != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FamilyRelation.entries.forEach { relation ->
                    FilterChip(
                        selected = profile.relationToHead == relation,
                        onClick = { onChange(profile.copy(relationToHead = relation)) },
                        label = { Text(relation.label) },
                        enabled = enabled,
                    )
                }
            }
        }
    }
}

/** Auswahl „Keine Familie“ oder eines Oberhaupts aus den Kandidaten. */
@Composable
private fun FamilyHeadSelector(family: EditorFamily, enabled: Boolean, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, enabled = enabled) {
            Text("Oberhaupt: ${family.head?.label ?: "keins"} ▾")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Keine Familie") },
                onClick = {
                    expanded = false
                    onSelect(null)
                },
            )
            family.candidates.forEach { candidate ->
                DropdownMenuItem(
                    text = { Text(candidate.label) },
                    onClick = {
                        expanded = false
                        onSelect(candidate.uid)
                    },
                )
            }
        }
    }
}

private val previewHead = AppUser(uid = "h", profile = MemberProfile(lastName = "Muster", firstName = "Max"))

@ThemePreviews
@Composable
private fun MemberFamilyFieldsRelativePreview() = PreviewThemeWrapper {
    MemberFamilyFields(
        profile = MemberProfile(familyHeadId = "h", relationToHead = FamilyRelation.SPOUSE),
        family = EditorFamily(head = previewHead, candidates = listOf(previewHead)),
        enabled = true,
        onChange = {},
    )
}

@ThemePreviews
@Composable
private fun MemberFamilyFieldsHeadPreview() = PreviewThemeWrapper {
    MemberFamilyFields(
        profile = MemberProfile(),
        family = EditorFamily(
            relatives = listOf(
                AppUser(uid = "a", profile = MemberProfile(lastName = "Muster", firstName = "Anna")),
                AppUser(uid = "t", profile = MemberProfile(lastName = "Muster", firstName = "Tim")),
            ),
        ),
        enabled = true,
        onChange = {},
    )
}

@ThemePreviews
@Composable
private fun MemberFamilyFieldsDisabledPreview() = PreviewThemeWrapper {
    MemberFamilyFields(profile = MemberProfile(), family = EditorFamily(), enabled = false, onChange = {})
}
