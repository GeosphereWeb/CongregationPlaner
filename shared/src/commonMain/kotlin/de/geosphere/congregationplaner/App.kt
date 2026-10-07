@file:Suppress("MatchingDeclarationName")

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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import congregationplaner.shared.generated.resources.Res
import congregationplaner.shared.generated.resources.dummy
import de.geosphere.congregationplaner.theming.AppTheme
import de.geosphere.congregationplaner.theming.brushes.backgroundBrush
import de.geosphere.congregationplaner.theming.customColors
import org.jetbrains.compose.resources.painterResource

@Suppress("LongMethod")
@Composable
fun App() {
    AppTheme {
        val authViewModel = viewModel { AuthViewModel() }
        val authState by authViewModel.uiState.collectAsState()
        var selectedRoute by remember { mutableStateOf("home") }

        LaunchedEffect(authState.isAuthenticated) {
            if (authState.isAuthenticated) {
                selectedRoute = "home"
            }
        }

        if (!authState.isAuthenticated) {
            LoginScreen(
                email = authState.email,
                password = authState.password,
                firebaseStatus = authState.firebaseStatus,
                authMode = authState.authMode,
                loginError = authState.loginError,
                infoMessage = authState.infoMessage,
                isLoading = authState.isLoading,
                onEmailChange = authViewModel::updateEmail,
                onPasswordChange = authViewModel::updatePassword,
                onToggleMode = authViewModel::toggleAuthMode,
                onLoginClick = authViewModel::authenticate,
                onGoogleClick = authViewModel::signInWithGoogle,
            )
            return@AppTheme
        }

        // Platform-spezifisches Layout
        if (HostPlatform.isDesktop) {
            DesktopLayout(
                selectedRoute = selectedRoute,
                firebaseStatus = authState.firebaseStatus,
                onRouteChange = { selectedRoute = it },
                onSignOut = authViewModel::signOut,
            )
        } else {
            MobileLayout(
                selectedRoute = selectedRoute,
                firebaseStatus = authState.firebaseStatus,
                onRouteChange = { selectedRoute = it },
                onSignOut = authViewModel::signOut,
            )
        }
    }
}

@Suppress("LongMethod", "LongParameterList", "MagicNumber")
@Composable
fun LoginScreen(
    email: String,
    password: String,
    firebaseStatus: String,
    authMode: AuthMode,
    loginError: String?,
    infoMessage: String?,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onLoginClick: () -> Unit,
    onGoogleClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(
            brush = Brush.linearGradient(listOf(Color(0xFF0B1220), Color(0xFF1E3A5F))),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.widthIn(min = 300.dp, max = 500.dp).padding(24.dp),
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
                    enabled = !isLoading,
                ) {
                    Text(
                        when {
                            isLoading -> "Bitte warten..."
                            authMode == AuthMode.LOGIN -> "Anmelden"
                            else -> "Konto erstellen"
                        },
                    )
                }

                Text(
                    text = "oder",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                Button(
                    onClick = onGoogleClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                ) {
                    Text("Mit Google fortfahren")
                }

                Button(
                    onClick = onToggleMode,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
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
fun DesktopLayout(
    selectedRoute: String,
    firebaseStatus: String,
    onRouteChange: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    Row(Modifier.fillMaxSize().background(brush = Brush.backgroundBrush)) {
        // Elegante, schlanke NavigationRail für Desktop
        NavigationRail(
            modifier = Modifier.padding(horizontal = 8.dp),
//            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            NavigationItem.entries.let { items ->
                items.forEach { item ->
                    NavigationRailItem(
                        icon = {
                            Icon(
                                painter = painterResource(item.iconRes),
                                contentDescription = null,
                                tint = MaterialTheme.customColors.success,
                            )
                        },
                        label = { Text(item.label) },
                        selected = selectedRoute == item.routeName,
                        onClick = { onRouteChange(item.routeName) },
                    )
                }
            }
            Button(onClick = onSignOut) {
                Text("Abmelden")
            }
        }

        // Hauptinhalt
        Scaffold(modifier = Modifier.weight(1f), containerColor = Color.Yellow) {
            Column(modifier = Modifier.fillMaxSize().padding(it).background(Brush.backgroundBrush)) {
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

@Preview(name = "Desktop Layout", widthDp = 1280, heightDp = 800)
@Composable
fun DesktopLayoutPreview() {
    AppTheme {
        DesktopLayout(
            selectedRoute = "home",
            firebaseStatus = "Firebase verfügbar",
            onRouteChange = {},
            onSignOut = {},
        )
    }
}

@Composable
fun MobileLayout(
    selectedRoute: String,
    firebaseStatus: String,
    onRouteChange: (String) -> Unit,
    onSignOut: () -> Unit,
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
                Button(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Text("Abmelden")
                }
            }
        },
        scrimColor = Color.Black.copy(alpha = 0.32f),
    ) {
        Scaffold {
            Box(Modifier.fillMaxSize().background(brush = Brush.backgroundBrush).padding(it)) {
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
}

@Preview(name = "Congregation Planer Login")
@Composable
fun AppPreview() {
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
            onGoogleClick = { },
        )
    }
}
