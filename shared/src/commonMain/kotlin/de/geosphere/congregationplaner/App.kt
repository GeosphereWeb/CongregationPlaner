package de.geosphere.congregationplaner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import congregationplaner.shared.generated.resources.Res
import congregationplaner.shared.generated.resources.dummy
import org.jetbrains.compose.resources.painterResource

@Composable
@Preview
fun App() {
    MaterialTheme {
        var selectedRoute by remember { mutableStateOf("home") }

        // Platform-spezifisches Layout
        if (HostPlatform.isDesktop) {
            DesktopLayout(selectedRoute) { selectedRoute = it }
        } else {
            MobileLayout(selectedRoute) { selectedRoute = it }
        }
    }
}

@Composable
fun DesktopLayout(
    selectedRoute: String,
    onRouteChange: (String) -> Unit,
) {
    Row {
        // Elegante, schlanke NavigationRail für Desktop
        NavigationRail(
            modifier = Modifier.width(80.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            NavigationItem.entries.let { items ->
                items.forEach { item ->
                    NavigationRailItem(
                        icon = {
                            Icon(
                                painter = painterResource(item.iconRes),
                                contentDescription = null,
                            )
                        },
                        label = { Text(item.label) },
                        selected = selectedRoute == item.routeName,
                        onClick = { onRouteChange(item.routeName) },
                    )
                }
            }
        }

        // Hauptinhalt
        Scaffold(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(it)) {
                when (selectedRoute) {
                    "home" -> Text("Home Content")
                    "settings" -> Text("Settings Content")
                    "leben_und_dienst" -> Text("leben_und_dienst \n Schätze \n uns verbessern \n leben als christ")
                    "planung_wochenende" -> Text("Vortragsplanung und WT Leiter")
                    "versammlung_metadata" ->
                        Text(
                            "versammlung_metadata \n versl_name \n vers_kalender mit Zeiten (f. planung)",
                        )
                    "dienste" -> Text("Diensteta")
                    "userverwaltung" -> Text("userverwaltung")
                    else -> Text("Select a navigation item")
                }
            }
        }
    }
}

@Composable
fun MobileLayout(
    selectedRoute: String,
    onRouteChange: (String) -> Unit,
) {
    var drawerOpen by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                Text("Congregation Planer", modifier = Modifier.padding(16.dp))
                HorizontalDivider()
                NavigationItem.entries.let { items ->
                    items.forEach { item ->
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    painter = painterResource(Res.drawable.dummy),
                                    contentDescription = null,
                                )
                            },
                            label = { Text(item.label) },
                            selected = selectedRoute == item.routeName,
                            onClick = {
                                onRouteChange(item.routeName)
                                drawerOpen = false
                            },
                        )
                    }
                }
            }
        },
        scrimColor = Color.Black.copy(alpha = 0.32f),
    ) {
        Scaffold {
            Column(modifier = Modifier.padding(it)) {
                when (selectedRoute) {
                    "home" -> Text("Home Content")
                    "settings" -> Text("Settings Content")
                    else -> Text("Select a navigation item")
                }
            }
        }
    }
}
