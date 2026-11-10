package de.geosphere.congregationplaner.talk.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.talk.domain.model.SpeakerContact
import de.geosphere.congregationplaner.theming.NightPreviewWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.chip.StatusTone
import de.geosphere.congregationplaner.ui.list.SwipeActionButton
import de.geosphere.congregationplaner.ui.list.SwipeableItemWithActions
import de.geosphere.congregationplaner.user.presentation.callIcon
import de.geosphere.congregationplaner.user.presentation.chatIcon

private const val CALL_LABEL = "Anrufen"
private const val WHATSAPP_LABEL = "WhatsApp"

/**
 * Ein Termin der Liste, der sich nach links wischen lässt, um den Redner anzurufen oder ihm per WhatsApp zu schreiben.
 * Ohne [contact] (kein Redner, keine Nummer oder keine Berechtigung) ist der Termin nicht wischbar. Für TalkBack
 * stehen dieselben Aktionen als Eintrag der Aktionsliste zur Verfügung.
 */
@Composable
internal fun SwipeableSlotItem(
    contact: SpeakerContact?,
    revealed: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (contact == null) {
        Box(modifier) { content() }
        return
    }
    val uriHandler = LocalUriHandler.current
    // Ein System ohne Telefon- oder WhatsApp-App lehnt den Link ab; dann passiert nichts.
    val open: (String) -> Unit = { uri ->
        onRevealedChange(false)
        runCatching { uriHandler.openUri(uri) }
    }
    val accessibilityActions = listOfNotNull(
        accessibilityAction(CALL_LABEL, contact.callUri, open),
        accessibilityAction(WHATSAPP_LABEL, contact.whatsAppUri, open),
    )
    SwipeableItemWithActions(
        isRevealed = revealed,
        onRevealedChange = onRevealedChange,
        modifier = modifier.semantics { customActions = accessibilityActions },
        endActions = { SlotContactActions(contact, open) },
        content = content,
    )
}

private fun accessibilityAction(label: String, uri: String?, open: (String) -> Unit): CustomAccessibilityAction? =
    uri?.let {
        CustomAccessibilityAction(label) {
            open(it)
            true
        }
    }

/** Die Icon-Buttons hinter dem Termin: Anrufen und, mit Handynummer, WhatsApp. */
@Composable
private fun RowScope.SlotContactActions(contact: SpeakerContact, onOpen: (String) -> Unit) {
    contact.callUri?.let { uri ->
        SwipeActionButton(callIcon, CALL_LABEL, StatusTone.Sky, onClick = { onOpen(uri) })
    }
    contact.whatsAppUri?.let { uri ->
        SwipeActionButton(chatIcon, WHATSAPP_LABEL, StatusTone.Mint, onClick = { onOpen(uri) })
    }
}

@ThemePreviews
@Composable
private fun SwipeableSlotItemPreview() = NightPreviewWrapper {
    Row {
        SwipeableSlotItem(
            contact = SpeakerContact(mobilePhone = "0170 1234567"),
            revealed = false,
            onRevealedChange = {},
        ) {
            Text("Mit Handynummer: nach links wischen", Modifier.fillMaxWidth().padding(16.dp))
        }
    }
}
