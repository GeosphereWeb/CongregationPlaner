package de.geosphere.congregationplaner.talk.presentation.schedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import de.geosphere.congregationplaner.user.presentation.contact.callIcon
import de.geosphere.congregationplaner.user.presentation.contact.smartphoneIcon
import de.geosphere.congregationplaner.user.presentation.contact.whatsAppIcon
private const val MOBILE_LABEL = "Handy anrufen"
private const val LANDLINE_LABEL = "Festnetz anrufen"
private const val WHATSAPP_LABEL = "WhatsApp"

/**
 * Ein Termin der Liste, hinter dem Aktionen für den Redner liegen: nach rechts wischen zeigt je ein Icon für Handy und
 * Festnetz (soweit die Nummer vorhanden ist), nach links wischen WhatsApp. [whatsAppMessages] sind die ausgefüllten
 * Vorlagen: Tippen öffnet die erste (den Standard), langes Drücken bietet alle zur Auswahl. Ohne [contact] (kein
 * Redner, keine Nummer oder keine Berechtigung) und zu einer Seite ohne Aktion lässt sich der Termin nicht wischen.
 * Für TalkBack stehen dieselben Aktionen als Eintrag der Aktionsliste zur Verfügung.
 */
@Composable
internal fun SwipeableSlotItem(
    contact: SpeakerContact?,
    whatsAppMessages: List<WhatsAppMessage>,
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
        accessibilityAction(MOBILE_LABEL, contact.mobileCallUri, open),
        accessibilityAction(LANDLINE_LABEL, contact.landlineCallUri, open),
    ) + whatsAppMessages.map { message ->
        CustomAccessibilityAction("$WHATSAPP_LABEL: ${message.name}") {
            open(message.uri)
            true
        }
    }
    SwipeableItemWithActions(
        isRevealed = revealed,
        onRevealedChange = onRevealedChange,
        modifier = modifier.semantics { customActions = accessibilityActions },
        startActions = if (contact.isReachable) ({ CallActions(contact, open) }) else null,
        endActions = whatsAppMessages.takeIf {
            it.isNotEmpty()
        }?.let { messages -> { WhatsAppAction(messages, open) } },
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

/** Nach rechts gewischt: Anrufen auf dem Handy und, wenn vorhanden, im Festnetz. */
@Composable
private fun RowScope.CallActions(contact: SpeakerContact, onOpen: (String) -> Unit) {
    contact.mobileCallUri?.let { uri ->
        SwipeActionButton(smartphoneIcon, MOBILE_LABEL, StatusTone.Sky, onClick = { onOpen(uri) })
    }
    contact.landlineCallUri?.let { uri ->
        SwipeActionButton(callIcon, LANDLINE_LABEL, StatusTone.Sky, onClick = { onOpen(uri) })
    }
}

/** Nach links gewischt: WhatsApp mit der Standardvorlage; langes Drücken zeigt alle Vorlagen. */
@Composable
private fun RowScope.WhatsAppAction(messages: List<WhatsAppMessage>, onOpen: (String) -> Unit) {
    var choosing by remember { mutableStateOf(false) }
    Box {
        SwipeActionButton(
            whatsAppIcon,
            "$WHATSAPP_LABEL: ${messages.first().name}",
            StatusTone.Mint,
            onClick = { onOpen(messages.first().uri) },
            onLongClick = { choosing = true }.takeIf { messages.size > 1 },
            longClickLabel = "Vorlage wählen",
        )
        DropdownMenu(expanded = choosing, onDismissRequest = { choosing = false }) {
            messages.forEach { message ->
                DropdownMenuItem(
                    text = { Text(message.name) },
                    onClick = {
                        choosing = false
                        onOpen(message.uri)
                    },
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun SwipeableSlotItemPreview() = NightPreviewWrapper {
    Column {
        SwipeableSlotItem(
            contact = SpeakerContact(mobilePhone = "0170 1234567", landlinePhone = "089 123456"),
            whatsAppMessages = listOf(
                WhatsAppMessage("Anfrage", "https://wa.me/491701234567?text=Hallo"),
                WhatsAppMessage("Erinnerung", "https://wa.me/491701234567?text=Erinnerung"),
            ),
            revealed = true,
            onRevealedChange = {},
        ) {
            Text("Handy und Festnetz: nach rechts, WhatsApp: nach links", Modifier.fillMaxWidth().padding(16.dp))
        }
        SwipeableSlotItem(
            contact = SpeakerContact(landlinePhone = "089 123456"),
            whatsAppMessages = emptyList(),
            revealed = false,
            onRevealedChange = {},
        ) {
            Text("Nur Festnetz: nur nach rechts", Modifier.fillMaxWidth().padding(16.dp))
        }
    }
}
