@file:Suppress("MatchingDeclarationName")

package de.geosphere.congregationplaner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import congregationplaner.shared.generated.resources.Res
import congregationplaner.shared.generated.resources.dummy
import de.geosphere.congregationplaner.auth.FirebaseAuthManager
import de.geosphere.congregationplaner.auth.FirebaseSupport
import de.geosphere.congregationplaner.circuit.presentation.CircuitRoute
import de.geosphere.congregationplaner.congregation.presentation.CongregationMetadataRoute
import de.geosphere.congregationplaner.organization.InviteLinkInbox
import de.geosphere.congregationplaner.organization.di.OrganizationGraph
import de.geosphere.congregationplaner.organization.presentation.CongregationSelector
import de.geosphere.congregationplaner.talk.presentation.ExternalDirectoryRoute
import de.geosphere.congregationplaner.talk.presentation.TalkHistoryRoute
import de.geosphere.congregationplaner.talk.presentation.TalkReferenceRoute
import de.geosphere.congregationplaner.talk.presentation.TalkScheduleRoute
import de.geosphere.congregationplaner.theming.AppTheme
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.NightPhonePreview
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.brushes.backgroundBrush
import de.geosphere.congregationplaner.theming.customColors
import de.geosphere.congregationplaner.theming.nightTextStyle
import de.geosphere.congregationplaner.ui.button.PrimaryButton
import de.geosphere.congregationplaner.ui.button.SecondaryButton
import de.geosphere.congregationplaner.ui.field.FieldBox
import de.geosphere.congregationplaner.ui.field.PasswordVisibilityButton
import de.geosphere.congregationplaner.ui.header.NightPanel
import de.geosphere.congregationplaner.ui.header.TeleprompterHeader
import de.geosphere.congregationplaner.user.di.UserAccessGraph
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserAccessState
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.presentation.UserAccessGateScreen
import de.geosphere.congregationplaner.user.presentation.UserManagementRoute
import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

enum class AuthMode {
    LOGIN,
    REGISTER,
}

@Suppress("LongMethod")
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
        val userAccessViewModel = remember { createGraph<UserAccessGraph>().userAccessViewModel }
        val selectionViewModel = remember { createGraph<OrganizationGraph>().congregationSelectionViewModel }
        val selectionState by selectionViewModel.uiState.collectAsState()
        val selectedCongregation = selectionState.selected?.ref
        val pendingInviteLink by InviteLinkInbox.pendingLink.collectAsState()

        LaunchedEffect(Unit) {
            FirebaseSupport.initialize()
            isAuthenticated = FirebaseAuthManager.isSignedIn()
            firebaseStatus = if (FirebaseSupport.isReady()) {
                "Firebase verfügbar"
            } else {
                "Firebase nicht konfiguriert"
            }
        }

        // Ein Einladungslink, mit dem die App gestartet wurde, wählt die Versammlung vor.
        LaunchedEffect(pendingInviteLink) {
            val link = pendingInviteLink ?: return@LaunchedEffect
            selectionViewModel.applyInviteLink(link)
            InviteLinkInbox.consume()
        }

        // Die Nutzerdaten liegen unterhalb der Versammlung; ohne gewählte Versammlung gibt es keinen Zugriff.
        LaunchedEffect(isAuthenticated, selectedCongregation) {
            if (!isAuthenticated) {
                userAccessViewModel.onSignedOut()
                return@LaunchedEffect
            }
            val authUser = FirebaseAuthManager.currentUser()
            if (authUser != null) {
                userAccessViewModel.onSignedIn(authUser, selectedCongregation)
            } else {
                FirebaseAuthManager.signOut()
                isAuthenticated = false
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
                canSignIn = selectedCongregation != null,
                showCredentials = selectedCongregation != null && !selectionState.isPicking,
                congregationName = selectionState.selected?.displayName,
                previousSteps = selectedCongregation?.let { listOf("Kreis ${it.circuitId}") }.orEmpty(),
                congregationSelector = { CongregationSelector(selectionViewModel, selectionState) },
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
                                infoMessage =
                                    "Registrierung erfolgreich. Bitte prüfe dein E-Mail-Postfach und bestätige " +
                                    "deine E-Mail-Adresse."
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
                onGoogleClick = {
                    scope.launch {
                        loginError = null
                        val user = FirebaseAuthManager.signInWithGoogle()
                        if (user != null) {
                            isAuthenticated = true
                            selectedRoute = "home"
                            email = ""
                            password = ""
                        } else {
                            loginError = "Google-Anmeldung fehlgeschlagen oder wurde abgebrochen."
                        }
                    }
                },
            )
            return@AppTheme
        }

        val signOut: () -> Unit = {
            scope.launch {
                FirebaseAuthManager.signOut()
                isAuthenticated = false
                authMode = AuthMode.LOGIN
                loginError = null
                infoMessage = null
            }
        }

        // Wer schon angemeldet ist, aber (noch) keine Versammlung gewählt hat, wählt sie hier.
        if (selectedCongregation == null) {
            CongregationRequiredScreen(onSignOut = signOut) {
                CongregationSelector(selectionViewModel, selectionState)
            }
            return@AppTheme
        }

        // Erst nach erteilter Freigabe ist die App nutzbar; Rollen steuern die sichtbaren Bereiche.
        val accessState by userAccessViewModel.accessState.collectAsState()
        val currentUser = (accessState as? UserAccessState.Authorized)?.user
        if (currentUser == null) {
            UserAccessGateScreen(
                state = accessState,
                onRefresh = userAccessViewModel::refresh,
                onChangeCongregation = selectionViewModel::clearSelection,
                onSignOut = signOut,
            )
            return@AppTheme
        }

        val navigationItems = remember(currentUser) {
            visibleNavigationItems(
                canManageUsers = userAccessViewModel.canManageUsers(currentUser),
                isGlobalAdmin = currentUser.isGlobalAdmin,
            )
        }
        LaunchedEffect(navigationItems, selectedRoute) {
            if (navigationItems.none { it.routeName == selectedRoute }) selectedRoute = NavigationItem.Home.routeName
        }

        // Platform-spezifisches Layout
        if (HostPlatform.isDesktop) {
            DesktopLayout(
                selectedRoute = selectedRoute,
                firebaseStatus = firebaseStatus,
                currentUser = currentUser,
                navigationItems = navigationItems,
                onRouteChange = { selectedRoute = it },
                onSignOut = signOut,
            )
        } else {
            MobileLayout(
                selectedRoute = selectedRoute,
                firebaseStatus = firebaseStatus,
                currentUser = currentUser,
                navigationItems = navigationItems,
                onRouteChange = { selectedRoute = it },
                onSignOut = signOut,
            )
        }
    }
}

/**
 * Zweistufige Anmeldung im Nachtblau-Design: erst die Versammlung (über [congregationSelector]),
 * danach Zugangsdaten. [congregationName] und [previousSteps] füllen den Kopfbereich in Schritt 2.
 */
@Suppress("LongMethod", "LongParameterList", "MagicNumber")
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
    onGoogleClick: () -> Unit,
    canSignIn: Boolean = true,
    showCredentials: Boolean = true,
    congregationName: String? = null,
    previousSteps: List<String> = emptyList(),
    congregationSelector: @Composable () -> Unit = {},
) {
    Box(
        modifier = Modifier.fillMaxSize().background(NightColors.Background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.widthIn(max = 480.dp).fillMaxHeight().imePadding()) {
            TeleprompterHeader(
                stepLabel = if (showCredentials) "SCHRITT 2 / 2" else "SCHRITT 1 / 2",
                current = if (showCredentials) congregationName ?: "Anmelden" else "Welche Versammlung?",
                previous = if (showCredentials) previousSteps else emptyList(),
            )
            NightPanel(modifier = Modifier.weight(1f)) {
                if (showCredentials) {
                    congregationSelector()
                    CredentialsForm(
                        email = email,
                        password = password,
                        authMode = authMode,
                        loginError = loginError,
                        infoMessage = infoMessage,
                        canSignIn = canSignIn,
                        onEmailChange = onEmailChange,
                        onPasswordChange = onPasswordChange,
                        onToggleMode = onToggleMode,
                        onLoginClick = onLoginClick,
                        onGoogleClick = onGoogleClick,
                    )
                } else {
                    PanelTitle("Versammlung wählen", "Wähle deine Versammlung, bevor du dich anmeldest.")
                    Spacer(Modifier.height(14.dp))
                    congregationSelector()
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Anmeldung folgt, sobald die Versammlung feststeht.",
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        textAlign = TextAlign.Center,
                        style = nightTextStyle(13.sp, color = NightColors.OnBackgroundMuted),
                    )
                }
                Text(
                    text = firebaseStatus,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    textAlign = TextAlign.Center,
                    style = nightTextStyle(11.sp, color = NightColors.OnBackgroundMuted, monospace = true),
                )
            }
        }
    }
}

@Composable
private fun PanelTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = nightTextStyle(28.sp, FontWeight.Bold, letterSpacing = (-0.56).sp))
        Text(subtitle, style = nightTextStyle(15.sp, color = NightColors.OnBackgroundMuted))
    }
}

/** Zweiter Schritt der Anmeldung: Zugangsdaten, sobald die Versammlung feststeht. */
@Suppress("LongMethod", "LongParameterList", "MagicNumber")
@Composable
private fun ColumnScope.CredentialsForm(
    email: String,
    password: String,
    authMode: AuthMode,
    loginError: String?,
    infoMessage: String?,
    canSignIn: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onLoginClick: () -> Unit,
    onGoogleClick: () -> Unit,
) {
    val isLogin = authMode == AuthMode.LOGIN
    var passwordVisible by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PanelTitle(
            title = if (isLogin) "Anmelden" else "Konto erstellen",
            subtitle = if (isLogin) "Weiter zu deiner Versammlung." else "Lege dein Konto für diese Versammlung an.",
        )
        FieldBox(
            label = "E-Mail",
            value = email,
            onValueChange = onEmailChange,
            placeholder = "name@beispiel.de",
            isError = loginError != null,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email,
            ),
        )
        FieldBox(
            label = "Passwort",
            value = password,
            onValueChange = onPasswordChange,
            isError = loginError != null,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = { PasswordVisibilityButton(passwordVisible, onToggle = { passwordVisible = !passwordVisible }) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
            ),
        )
        if (loginError != null) {
            Text(loginError, style = nightTextStyle(14.sp, color = NightColors.Coral))
        }
        if (infoMessage != null) {
            Text(infoMessage, style = nightTextStyle(14.sp, color = NightColors.Sky))
        }
        PrimaryButton(
            text = if (isLogin) "Anmelden" else "Konto erstellen",
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = canSignIn,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = NightColors.Border)
            Text("oder", style = nightTextStyle(13.sp, FontWeight.Medium, NightColors.OnBackgroundMuted))
            HorizontalDivider(modifier = Modifier.weight(1f), color = NightColors.Border)
        }
        SecondaryButton(
            text = "Mit Google fortfahren",
            onClick = onGoogleClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = canSignIn,
            height = 56.dp,
        )
    }
    Spacer(Modifier.weight(1f))
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (isLogin) "Noch kein Konto?" else "Bereits registriert?",
            style = nightTextStyle(14.sp, color = NightColors.OnBackgroundMuted),
        )
        SecondaryButton(
            text = if (isLogin) "Neues Konto erstellen" else "Anmelden",
            onClick = onToggleMode,
            outlined = false,
            contentColor = NightColors.OnBackground,
        )
    }
}

/** Für bereits angemeldete Nutzer, deren Versammlung noch nicht feststeht (z. B. nach einem Update). */
@Composable
private fun CongregationRequiredScreen(onSignOut: () -> Unit, selector: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(brush = Brush.backgroundBrush)
            .verticalScroll(rememberScrollState()).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Card(
            modifier = Modifier.widthIn(min = 300.dp, max = 500.dp).padding(24.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Versammlung wählen", style = MaterialTheme.typography.headlineSmall)
                selector()
                Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                    Text("Abmelden")
                }
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
fun DesktopLayout(
    selectedRoute: String,
    firebaseStatus: String,
    currentUser: AppUser,
    navigationItems: List<NavigationItem>,
    onRouteChange: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    Row(Modifier.fillMaxSize().background(brush = Brush.backgroundBrush)) {
        // Elegante, schlanke NavigationRail für Desktop
        NavigationRail(
            modifier = Modifier.padding(horizontal = 8.dp),
//            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            navigationItems.let { items ->
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

                    "planung_wochenende" -> TalkScheduleRoute(currentUser)

                    "talk_history" -> TalkHistoryRoute(currentUser)

                    "external_speakers" -> ExternalDirectoryRoute(currentUser)

                    "talk_reference" -> TalkReferenceRoute(currentUser)

                    "circuit" -> CircuitRoute(currentUser)

                    "versammlung_metadata" -> CongregationMetadataRoute(currentUser)

                    "dienste" -> Text("Diensteta")

                    "userverwaltung" -> UserManagementRoute(currentUser)

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
            currentUser = previewAdmin,
            navigationItems = NavigationItem.entries,
            onRouteChange = {},
            onSignOut = {},
        )
    }
}

@Composable
@Suppress("LongParameterList")
fun MobileLayout(
    selectedRoute: String,
    firebaseStatus: String,
    currentUser: AppUser,
    navigationItems: List<NavigationItem>,
    onRouteChange: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    var drawerOpen by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                Text("Congregation Planer", modifier = Modifier.padding(16.dp))
                HorizontalDivider()
                navigationItems.let { items ->
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
                        "planung_wochenende" -> TalkScheduleRoute(currentUser)
                        "talk_history" -> TalkHistoryRoute(currentUser)
                        "external_speakers" -> ExternalDirectoryRoute(currentUser)
                        "talk_reference" -> TalkReferenceRoute(currentUser)
                        "circuit" -> CircuitRoute(currentUser)
                        "versammlung_metadata" -> CongregationMetadataRoute(currentUser)
                        "userverwaltung" -> UserManagementRoute(currentUser)
                        else -> Text("Select a navigation item")
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginPreviewContent(authMode: AuthMode, loginError: String? = null) = NightPhonePreview {
    LoginScreen(
        email = "name@beispiel.de",
        password = "geheimeswort",
        firebaseStatus = "Firebase verfügbar",
        authMode = authMode,
        loginError = loginError,
        infoMessage = null,
        onEmailChange = {},
        onPasswordChange = {},
        onToggleMode = {},
        onLoginClick = {},
        onGoogleClick = {},
        congregationName = "Berlin-Mitte",
        previousSteps = listOf("Kreis 609a"),
    )
}

@ThemePreviews
@Composable
private fun LoginPreview() = LoginPreviewContent(AuthMode.LOGIN)

@ThemePreviews
@Composable
private fun LoginRegisterPreview() = LoginPreviewContent(AuthMode.REGISTER)

@ThemePreviews
@Composable
private fun LoginErrorPreview() = LoginPreviewContent(AuthMode.LOGIN, loginError = "E-Mail oder Passwort ist falsch.")

@Preview(name = "Mobile Layout", widthDp = 400, heightDp = 850)
@Composable
fun MobileLayoutPreview() {
    AppTheme {
        MobileLayout(
            selectedRoute = "home",
            firebaseStatus = "Firebase verfügbar",
            currentUser = previewAdmin,
            navigationItems = NavigationItem.entries,
            onRouteChange = {},
            onSignOut = {},
        )
    }
}

private val previewAdmin = AppUser(
    uid = "preview-admin",
    email = "admin@congregationplaner.de",
    displayName = "Admin",
    approved = true,
    role = UserRole.CONGREGATION_ADMIN,
    isGlobalAdmin = true,
)
