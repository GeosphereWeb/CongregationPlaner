package de.geosphere.congregationplaner.talk.presentation.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.theming.PreviewThemeWrapper
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.ui.form.SwitchRow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** Schalter „Angefragt“ im Termin-Editor; ohne Redner oder bei entfallendem Vortrag lässt er sich nicht einschalten. */
@Composable
internal fun RequestSwitch(slot: TalkSlot, onRequestedChange: (Boolean) -> Unit) {
    SwitchRow(
        label = "Angefragt (Antwort offen)",
        checked = slot.isRequested,
        enabled = slot.canBeRequested || slot.isRequested,
        onCheckedChange = onRequestedChange,
    )
}

@ThemePreviews
@Composable
private fun RequestSwitchPreview() = PreviewThemeWrapper {
    val slot = TalkSlot(LocalDate(2026, 12, 6), LocalTime(10, 0), speakerLastName = "Muster", speakerFirstName = "Max")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RequestSwitch(slot.copy(requestId = "r1"), onRequestedChange = {})
        RequestSwitch(slot, onRequestedChange = {})
        RequestSwitch(slot.copy(speakerLastName = null, speakerFirstName = null), onRequestedChange = {})
    }
}
