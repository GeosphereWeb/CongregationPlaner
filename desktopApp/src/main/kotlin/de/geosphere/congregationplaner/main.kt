package de.geosphere.congregationplaner

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import congregationplaner.shared.generated.resources.Res
import congregationplaner.shared.generated.resources.dummy
import de.geosphere.congregationplaner.auth.data.FirebaseSupport
import de.geosphere.congregationplaner.auth.presentation.LoginActions
import de.geosphere.congregationplaner.auth.presentation.LoginContent
import de.geosphere.congregationplaner.auth.presentation.LoginForm
import de.geosphere.congregationplaner.auth.presentation.LoginStep
import de.geosphere.congregationplaner.auth.presentation.LoginUiState
import de.geosphere.congregationplaner.organization.presentation.selection.InviteLinkInbox
import de.geosphere.congregationplaner.theming.AppTheme
import org.jetbrains.compose.resources.painterResource

fun main(args: Array<String>) {
    FirebaseSupport.initialize()
    // Ein Einladungslink kann als Startargument übergeben werden.
    InviteLinkInbox.deliver(args.firstOrNull())

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
}

@Composable
@Preview(name = "Login Screen (Desktop)", device = Devices.DESKTOP)
fun LoginScreenDesktopPreview() {
    AppTheme {
        LoginContent(
            state = LoginUiState(form = LoginForm(email = "demo@congregationplaner.de", password = "Passwort123")),
            step = LoginStep(stepLabel = "SCHRITT 2 / 2", title = "Anmelden", showCredentials = true, canSignIn = true),
            firebaseStatus = "Firebase verfügbar",
            actions = LoginActions(),
        )
    }
}
