package de.geosphere.congregationplaner

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import congregationplaner.shared.generated.resources.Res
import congregationplaner.shared.generated.resources.dummy
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Congregation Planer",
        icon = painterResource(Res.drawable.dummy),
    ) {
        MenuBar {
            Menu("Datei") {
                Item(
                    "Neu",
                    onClick = { /* Aktion */ },
                    icon = painterResource(Res.drawable.dummy)
                )
                Item(
                    "Öffnen",
                    onClick = { /* Aktion */ },
                    icon = painterResource(Res.drawable.dummy)
                )
                Item("Speichern", onClick = { /* Aktion */ })
                Separator()
                Item("Beenden", onClick = ::exitApplication)
            }
            Menu("Bearbeiten") {
                Item("Kopieren", onClick = { /* Aktion */ })
                Item("Einfügen", onClick = { /* Aktion */ })
            }
        }
        App()
    }
}

@Composable
@Preview(device = Devices.DESKTOP)
fun werner() {
    App()
}
