package de.geosphere.congregationplaner.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.button.DangerButton
import de.geosphere.congregationplaner.ui.button.PrimaryButton
import de.geosphere.congregationplaner.ui.button.SecondaryButton
import de.geosphere.congregationplaner.ui.card.FieldCard
import de.geosphere.congregationplaner.ui.field.AppSwitch

private val COMPACT_WIDTH = 600.dp
private val BUTTON_HEIGHT = 48.dp

@Composable
fun SwitchRow(label: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        AppSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

/** „Ändern“/„Wählen“ und, falls etwas gewählt ist, „Entfernen“ rechts in einer [FieldCard]. */
@Composable
fun FieldActions(changeLabel: String, enabled: Boolean, onChange: () -> Unit, onRemove: (() -> Unit)?) {
    Column(horizontalAlignment = Alignment.End) {
        SecondaryButton(changeLabel, onChange, enabled = enabled, outlined = false)
        if (onRemove != null) TextButton(onClick = onRemove) { Text("Entfernen") }
    }
}

/**
 * Speichern, Verwerfen und (mit [onDelete]) Löschen eines Eintrags in der Detailansicht: auf dem Handy volle Breite
 * untereinander, sonst nebeneinander rechts; „Löschen“ steht als roter Knopf in derselben Breite darunter.
 */
@Composable
@Suppress("LongParameterList")
fun EditorActions(
    saveText: String,
    saveEnabled: Boolean,
    discardEnabled: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDelete: (() -> Unit)? = null,
    deleteText: String = "Löschen",
    deleteEnabled: Boolean = true,
) {
    val delete: @Composable () -> Unit = {
        if (onDelete != null) {
            DangerButton(deleteText, onDelete, Modifier.fillMaxWidth(), deleteEnabled, BUTTON_HEIGHT)
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < COMPACT_WIDTH) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(saveText, onSave, Modifier.fillMaxWidth(), saveEnabled, BUTTON_HEIGHT)
                SecondaryButton(
                    "Verwerfen",
                    onDiscard,
                    Modifier.fillMaxWidth(),
                    discardEnabled,
                    height = BUTTON_HEIGHT,
                )
                delete()
            }
        } else {
            // Löschen so breit wie Verwerfen und Speichern zusammen.
            Column(
                modifier = Modifier.align(Alignment.TopEnd).width(IntrinsicSize.Max),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("Verwerfen", onDiscard, enabled = discardEnabled, height = BUTTON_HEIGHT)
                    PrimaryButton(saveText, onSave, enabled = saveEnabled, height = BUTTON_HEIGHT)
                }
                delete()
            }
        }
    }
}

@ThemePreviews
@Composable
private fun EditorControlsPreview() = PreviewThemeWrapper {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SwitchRow(label = "Informiert", checked = true) {}
        SwitchRow(label = "Vortrag entfällt", checked = false, enabled = false) {}
        FieldActions(changeLabel = "Ändern", enabled = true, onChange = {}, onRemove = {})
        EditorActions(saveText = "Speichern", saveEnabled = true, discardEnabled = true, onSave = {
        }, onDiscard = {}, onDelete = {})
    }
}
