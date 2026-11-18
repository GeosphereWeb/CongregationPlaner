package de.geosphere.congregationplaner.event.presentation

import congregationplaner.uilib.generated.resources.Res
import congregationplaner.uilib.generated.resources.allDrawableResources
import congregationplaner.uilib.generated.resources.ic_cp_square
import org.jetbrains.compose.resources.DrawableResource

/** Die Grafik zum gespeicherten Icon-Namen einer Ereignisart (ADR-0036); unbekannt oder fehlend ergibt das Quadrat. */
internal fun eventTypeIcon(name: String?): DrawableResource =
    name?.let { Res.allDrawableResources[it] } ?: Res.drawable.ic_cp_square
