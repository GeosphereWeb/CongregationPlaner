package de.geosphere.congregationplaner

import congregationplaner.shared.generated.resources.Res
import congregationplaner.shared.generated.resources.dummy
import org.jetbrains.compose.resources.DrawableResource

enum class NavigationItem(
    val iconRes: DrawableResource,
    val label: String,
    val routeName: String,
) {
    Home(
        iconRes = Res.drawable.dummy,
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
}
