package de.geosphere.congregationplaner.user.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightCardColors
import de.geosphere.congregationplaner.ui.button.PrimaryButton
import de.geosphere.congregationplaner.user.di.OwnProfileGraph
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.DEFAULT_MESSAGE_TEMPLATES
import de.geosphere.congregationplaner.user.domain.model.MessagePlaceholder
import de.geosphere.congregationplaner.user.domain.model.MessageTemplate
import de.geosphere.congregationplaner.user.domain.usecase.canKeepMessageTemplates
import de.geosphere.congregationplaner.user.presentation.contact.ProfileField
import dev.zacsweers.metro.createGraph

/** „WhatsApp-Vorlagen“ unter „Einstellungen“; nur für Mitglieder mit eigenem Konto in einer Versammlung. */
@Composable
internal fun MessageTemplatesSection(user: AppUser, modifier: Modifier = Modifier) {
    if (!user.canKeepMessageTemplates) return
    val viewModel = remember { createGraph<OwnProfileGraph>().messageTemplatesViewModel }
    LaunchedEffect(viewModel, user) { viewModel.onUserChanged(user) }
    val state by viewModel.uiState.collectAsState()
    MessageTemplatesForm(
        state = state,
        actions = MessageTemplatesActions(
            onChange = viewModel::onTemplateChange,
            onAdd = viewModel::addTemplate,
            onRemove = viewModel::removeTemplate,
            onMakeDefault = viewModel::makeDefault,
            onSave = viewModel::save,
        ),
        modifier = modifier,
    )
}

/** Callbacks der Vorlagen; [onChange], [onRemove] und [onMakeDefault] bekommen die Stelle in der Liste. */
internal data class MessageTemplatesActions(
    val onChange: (Int, MessageTemplate) -> Unit = { _, _ -> },
    val onAdd: () -> Unit = {},
    val onRemove: (Int) -> Unit = {},
    val onMakeDefault: (Int) -> Unit = {},
    val onSave: () -> Unit = {},
)

@Composable
private fun MessageTemplatesForm(
    state: MessageTemplatesUiState,
    actions: MessageTemplatesActions,
    modifier: Modifier = Modifier,
) {
    val enabled = !state.isSaving
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("WhatsApp-Vorlagen", style = MaterialTheme.typography.titleMedium)
        Text(
            "Beim Wischen eines Termins nach links öffnet WhatsApp mit der Standardvorlage; langes Drücken zeigt " +
                "alle Vorlagen. Platzhalter: " +
                MessagePlaceholder.entries.joinToString(", ") { "${it.key} (${it.description})" },
            style = MaterialTheme.typography.bodySmall,
            color = NightColors.OnBackgroundMuted,
        )
        state.form.forEachIndexed { index, template ->
            TemplateCard(index, template, isDefault = index == 0, enabled, state.canRemove, actions)
        }
        if (state.canAdd) TextButton(onClick = actions.onAdd) { Text("Vorlage hinzufügen") }
        state.message?.let { message ->
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.isError) MaterialTheme.colorScheme.error else NightColors.OnBackgroundMuted,
            )
        }
        PrimaryButton(
            text = if (state.isSaving) "Speichert …" else "Vorlagen speichern",
            onClick = actions.onSave,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun TemplateCard(
    index: Int,
    template: MessageTemplate,
    isDefault: Boolean,
    enabled: Boolean,
    canRemove: Boolean,
    actions: MessageTemplatesActions,
) {
    Card(colors = nightCardColors()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileField(template.name, "Name", enabled, { actions.onChange(index, template.copy(name = it)) })
            OutlinedTextField(
                value = template.text,
                onValueChange = { actions.onChange(index, template.copy(text = it)) },
                label = { Text("Text") },
                enabled = enabled,
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDefault) {
                    Text("Standard", color = NightColors.Mint)
                } else {
                    TextButton(onClick = { actions.onMakeDefault(index) }, enabled = enabled) { Text("Als Standard") }
                }
                Spacer(Modifier.weight(1f))
                if (canRemove) {
                    TextButton(onClick = { actions.onRemove(index) }) { Text("Entfernen") }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun MessageTemplatesFormPreview() = PreviewThemeWrapper {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        MessageTemplatesForm(
            state = MessageTemplatesUiState(
                form = DEFAULT_MESSAGE_TEMPLATES.take(2),
                saved = DEFAULT_MESSAGE_TEMPLATES,
            ),
            actions = MessageTemplatesActions(),
        )
        MessageTemplatesForm(
            state = MessageTemplatesUiState(
                form = DEFAULT_MESSAGE_TEMPLATES.take(1),
                saved = DEFAULT_MESSAGE_TEMPLATES.take(1),
                message = "Gespeichert.",
            ),
            actions = MessageTemplatesActions(),
        )
    }
}
