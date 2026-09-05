package de.geosphere.congregationplaner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import congregationplaner.shared.generated.resources.Res
import congregationplaner.shared.generated.resources.dummy
import de.geosphere.congregationplaner.theming.AppTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

enum class AuthMode {
    LOGIN,
    REGISTER,
}

@Composable
fun App() {
    AppTheme {
        var selectedRoute by remember { mutableStateOf("home") }
        var firebaseStatus by remember { mutableStateOf("Firebase wird initialisiert...") }
        var isAuthenticated by remember { mutableStateOf(FirebaseAuthManager.isSignedIn()) }
        var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
        var loginError by remember { mutableStateOf<String?>(null) }
        var infoMessage by remember { mutableStateOf<String?>(null) }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            FirebaseSupport.initialize()
            isAuthenticated = FirebaseAuthManager.isSignedIn()
            firebaseStatus = if (FirebaseSupport.isReady()) {
                "Firebase verfügbar"
            } else {
                "Firebase nicht konfiguriert"
            }
        }

        if (!isAuthenticated) {
            LoginScreen(
                email = email,
                password = password,
                firebaseStatus = firebaseStatus,
                authMode = authMode,
                loginError = loginError,
                infoMessage = infoMessage,
                onEmailChange = { email = it },
                onPasswordChange = { password = it },
                onToggleMode = {
                    authMode = if (authMode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN
                    loginError = null
                    infoMessage = null
                },
                onLoginClick = {
                    scope.launch {
                        loginError = null
                        val user = if (authMode == AuthMode.REGISTER) {
                            FirebaseAuthManager.createUserWithEmailAndPassword(email.trim(), password)
                        } else {
                            FirebaseAuthManager.signInWithEmailAndPassword(email.trim(), password)
                        }

                        if (user != null) {
                            if (authMode == AuthMode.REGISTER) {
                                infoMessage = "Registrierung erfolgreich. Bitte prüfe dein E-Mail-Postfach und bestätige deine E-Mail-Adresse."
                                authMode = AuthMode.LOGIN
                                email = ""
                                password = ""
                            } else {
                                isAuthenticated = true
                                selectedRoute = "home"
                                email = ""
                                password = ""
                            }
                        } else {
                            val failureMessage = if (authMode == AuthMode.REGISTER) {
                                "Registrierung fehlgeschlagen. Bitte prüfe deine Eingaben."
                            } else {
                                "Login fehlgeschlagen. Bitte E-Mail und Passwort prüfen."
                            }
                            loginError = failureMessage
                        }
                    }
                },
            )
            return@AppTheme
        }

        // Platform-spezifisches Layout
        if (HostPlatform.isDesktop) {
            DesktopLayout(selectedRoute, firebaseStatus) { selectedRoute = it }
        } else {
            MobileLayout(selectedRoute, firebaseStatus) { selectedRoute = it }
        }
    }
}

@Composable
fun LoginScreen(
    email: String,
    password: String,
    firebaseStatus: String,
    authMode: AuthMode,
    loginError: String?,
    infoMessage: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onLoginClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(brush = Brush.linearGradient(listOf(Color(0xFF0B1220), Color(0xFF1E3A5F)))),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Congregation Planer",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(text = firebaseStatus)

                val title = if (authMode == AuthMode.LOGIN) "Anmeldung" else "Konto erstellen"
                Text(title)

                Text("E-Mail")
                BasicTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(48.dp).border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(12.dp),
                    ).padding(horizontal = 12.dp, vertical = 10.dp),
                )

                Text("Passwort")
                BasicTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(48.dp).border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(12.dp),
                    ).padding(horizontal = 12.dp, vertical = 10.dp),
                    visualTransformation = PasswordVisualTransformation(),
                )

                if (loginError != null) {
                    Text(
                        text = loginError,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                if (infoMessage != null) {
                    Text(
                        text = infoMessage,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Button(
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text(if (authMode == AuthMode.LOGIN) "Anmelden" else "Konto erstellen")
                }

                Button(
                    onClick = onToggleMode,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (authMode == AuthMode.LOGIN) "Neues Konto erstellen" else "Bereits registriert? Anmelden",
                    )
                }
            }
        }
    }
}

@Composable
fun DesktopLayout(selectedRoute: String, firebaseStatus: String, onRouteChange: (String) -> Unit) {
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
                Text(firebaseStatus)
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
fun MobileLayout(selectedRoute: String, firebaseStatus: String, onRouteChange: (String) -> Unit) {
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
                Text(firebaseStatus)
                when (selectedRoute) {
                    "home" -> Text("Home Content")
                    "settings" -> Text("Settings Content")
                    else -> Text("Select a navigation item")
                }
            }
        }
    }
}
