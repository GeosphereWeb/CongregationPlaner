package de.geosphere.congregationplaner

import congregationplaner.shared.generated.resources.Res
import congregationplaner.shared.generated.resources.allDrawableResources
import congregationplaner.shared.generated.resources.dummy
import org.jetbrains.compose.resources.DrawableResource

fun resolveHomeIcon(drawableResources: Map<String, DrawableResource> = Res.allDrawableResources): DrawableResource =
    drawableResources["arena.xml"] ?: Res.drawable.dummy

enum class NavigationItem(val iconRes: DrawableResource, val label: String, val routeName: String) {
    Home(
        iconRes = resolveHomeIcon(),
        label = "Home",
        routeName = "home",
    ),
    Settings(
        iconRes = Res.drawable.dummy,
        label = "Settings",
        routeName = "settings",
    ),
    PlanungUnterDerWoche(
        iconRes = Res.drawable.dummy,
        label = "Planung unter der Woche",
        routeName = "leben_und_dienst",
    ),
    PlanungUnterWochenende(
        iconRes = Res.drawable.dummy,
        label = "Planung Wochenende",
        routeName = "planung_wochenende",
    ),
    Kreise(
        iconRes = Res.drawable.dummy,
        label = "Kreise",
        routeName = "circuit",
    ),
    Versammlung(
        iconRes = Res.drawable.dummy,
        label = "Versammlung Metadaten",
        routeName = "versammlung_metadata",
    ),
    Dienste(
        iconRes = Res.drawable.dummy,
        label = "Dienste",
        routeName = "dienste",
    ),
    Usererwaltung(
        iconRes = Res.drawable.dummy,
        label = "Userverwaltung",
        routeName = "userverwaltung",
    ),
    Vortragsdaten(
        iconRes = Res.drawable.dummy,
        label = "Vortragsliste & Ereignisse",
        routeName = "talk_reference",
    ),
}

/** Die Menüpunkte, die der Nutzer sehen darf: Userverwaltung für Admins, Vortragsdaten für globale Admins. */
fun visibleNavigationItems(canManageUsers: Boolean, isGlobalAdmin: Boolean): List<NavigationItem> =
    NavigationItem.entries.filter { item ->
        when (item) {
            NavigationItem.Usererwaltung -> canManageUsers
            NavigationItem.Vortragsdaten -> isGlobalAdmin
            else -> true
        }
    }
