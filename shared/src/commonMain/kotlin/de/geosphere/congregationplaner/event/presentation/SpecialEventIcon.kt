package de.geosphere.congregationplaner.event.presentation

import congregationplaner.uilib.generated.resources.Res
import congregationplaner.uilib.generated.resources.ic_cp_arena_duo
import congregationplaner.uilib.generated.resources.ic_cp_persons_doorstep_duo
import congregationplaner.uilib.generated.resources.ic_cp_square
import congregationplaner.uilib.generated.resources.ic_cp_wine_bread_duo
import org.jetbrains.compose.resources.DrawableResource

private val iconsByKeyword = listOf(
    "kreiskongress" to Res.drawable.ic_cp_arena_duo,
    "abendmahl" to Res.drawable.ic_cp_wine_bread_duo,
    "gedächtnismahl" to Res.drawable.ic_cp_wine_bread_duo,
    "dienstwoche" to Res.drawable.ic_cp_persons_doorstep_duo,
)

/** Icon eines besonderen Ereignisses nach Stichwort im Titel (ohne Groß-/Kleinschreibung); sonst ein Quadrat. */
internal fun specialEventIcon(title: String): DrawableResource {
    val lower = title.lowercase()
    return iconsByKeyword.firstOrNull { (keyword, _) -> keyword in lower }?.second ?: Res.drawable.ic_cp_square
}
