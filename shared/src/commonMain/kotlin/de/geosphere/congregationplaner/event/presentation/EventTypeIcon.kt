package de.geosphere.congregationplaner.event.presentation

import congregationplaner.uilib.generated.resources.Res
import congregationplaner.uilib.generated.resources.allDrawableResources
import congregationplaner.uilib.generated.resources.ic_cp_square
import org.jetbrains.compose.resources.DrawableResource

private const val ICON_PREFIX = "ic_cp_"

/** Die Grafik zum gespeicherten Icon-Namen einer Ereignisart (ADR-0036); unbekannt oder fehlend ergibt das Quadrat. */
internal fun eventTypeIcon(name: String?): DrawableResource =
    name?.let { Res.allDrawableResources[it] } ?: Res.drawable.ic_cp_square

/** Alle wählbaren Icons des App-Satzes, nach Name sortiert. */
internal val eventIconNames: List<String> by lazy {
    Res.allDrawableResources.keys.filter { it.startsWith(ICON_PREFIX) }.sorted()
}

/** Der Name ohne Präfix und mit Leerzeichen, z. B. „arena duo“; so zeigt und durchsucht ihn die Auswahl. */
internal fun iconLabel(name: String): String = name.removePrefix(ICON_PREFIX).replace('_', ' ')

/** Icons, deren Bezeichnung alle Wörter der Suche enthält (ohne Groß-/Kleinschreibung); leere Suche zeigt alle. */
internal fun List<String>.matchingIcons(query: String): List<String> {
    val words = query.lowercase().split(' ', '_').filter(String::isNotBlank)
    return filter { name -> words.all { it in iconLabel(name) } }
}
