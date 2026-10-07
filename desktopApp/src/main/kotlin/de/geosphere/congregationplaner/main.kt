package de.geosphere.congregationplaner

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import congregationplaner.shared.generated.resources.Res
import congregationplaner.shared.generated.resources.dummy
import de.geosphere.congregationplaner.auth.FirebaseSupport
import de.geosphere.congregationplaner.theming.AppTheme
import org.jetbrains.compose.resources.painterResource

fun main() {
    FirebaseSupport.initialize()

    application {
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
                        icon = painterResource(Res.drawable.dummy),
                    )
                    Item(
                        "Öffnen",
                        onClick = { /* Aktion */ },
                        icon = painterResource(Res.drawable.dummy),
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
}

@Composable
@Preview(name = "Login Screen (Desktop)", device = Devices.DESKTOP)
fun LoginScreenDesktopPreview() {
    AppTheme {
        LoginScreen(
            email = "demo@congregationplaner.de",
            password = "Passwort123",
            firebaseStatus = "Firebase verfügbar",
            authMode = AuthMode.LOGIN,
            loginError = null,
            infoMessage = null,
            isLoading = false,
            onEmailChange = {},
            onPasswordChange = {},
            onToggleMode = {},
            onLoginClick = {},
            onGoogleClick = {},
        )
    }
}
