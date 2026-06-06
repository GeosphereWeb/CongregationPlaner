package de.geosphere.congregationplaner

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.painterResource
import congregationplaner.shared.generated.resources.Res
import congregationplaner.shared.generated.resources.kontakt

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Congregation Planer",
        icon = painterResource(Res.drawable.kontakt)
    ) {
        App()
    }
}