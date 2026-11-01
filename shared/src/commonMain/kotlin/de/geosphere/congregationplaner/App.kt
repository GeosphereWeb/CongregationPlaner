@file:Suppress("MatchingDeclarationName")

package de.geosphere.congregationplaner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.geosphere.congregationplaner.auth.FirebaseAuthManager
import de.geosphere.congregationplaner.auth.FirebaseSupport
import de.geosphere.congregationplaner.organization.InviteLinkInbox
import de.geosphere.congregationplaner.organization.di.OrganizationGraph
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.model.SelectedCongregation
import de.geosphere.congregationplaner.organization.presentation.CongregationSelectionUiState
import de.geosphere.congregationplaner.organization.presentation.CongregationSelector
import de.geosphere.congregationplaner.organization.presentation.OrgLevelSelection
import de.geosphere.congregationplaner.presentation.navigation.AppNavigationActions
import de.geosphere.congregationplaner.presentation.navigation.AppNavigationGraph
import de.geosphere.congregationplaner.presentation.navigation.AppScaffold
import de.geosphere.congregationplaner.theming.AppTheme
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.NightPhonePreview
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightTextStyle
import de.geosphere.congregationplaner.ui.button.PrimaryButton
import de.geosphere.congregationplaner.ui.button.SecondaryButton
import de.geosphere.congregationplaner.ui.field.FieldBox
import de.geosphere.congregationplaner.ui.field.PasswordVisibilityButton
import de.geosphere.congregationplaner.ui.header.NightScreen
import de.geosphere.congregationplaner.ui.header.PanelTitle
import de.geosphere.congregationplaner.user.di.UserAccessGraph
import de.geosphere.congregationplaner.user.domain.model.UserAccessState
import de.geosphere.congregationplaner.user.presentation.UserAccessGateScreen
import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN,
    REGISTER,
}

@Suppress("LongMethod")
@Composable
fun App() {
    AppTheme {
        var firebaseStatus by remember { mutableStateOf("Firebase wird initialisiert...") }
        var isAuthenticated by remember { mutableStateOf(FirebaseAuthManager.isSignedIn()) }
        var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
        var globalLogin by remember { mutableStateOf(false) }
        var loginError by remember { mutableStateOf<String?>(null) }
        var infoMessage by remember { mutableStateOf<String?>(null) }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()
        val userAccessViewModel = remember { createGraph<UserAccessGraph>().userAccessViewModel }
        val selectionViewModel = remember { createGraph<OrganizationGraph>().congregationSelectionViewModel }
        val navigationViewModel = remember { createGraph<AppNavigationGraph>().appNavigationViewModel }
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

        // Die Nutzerdaten liegen unterhalb der Versammlung; ohne gewählte Versammlung gibt es keinen Zugriff, außer für
        // globale Admins (ADR-0013).
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
                canSignIn = selectedCongregation != null || globalLogin,
                showCredentials = globalLogin || (selectedCongregation != null && !selectionState.isPicking),
                congregationName = if (globalLogin) "Globale Verwaltung" else selectionState.selected?.displayName,
                previousSteps = if (globalLogin) {
                    emptyList()
                } else {
                    selectedCongregation?.let { listOf("Kreis ${it.circuitId}") }.orEmpty()
                },
                congregationSelector = {
                    if (globalLogin) {
                        GlobalLoginHint(onChooseCongregation = { globalLogin = false })
                    } else {
                        CongregationSelector(selectionViewModel, selectionState)
                    }
                },
                onGlobalAdminLogin = { globalLogin = true },
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
                globalLogin = false
                loginError = null
                infoMessage = null
            }
        }

        // Erst nach erteilter Freigabe ist die App nutzbar; Rollen steuern die sichtbaren Bereiche.
        val accessState by userAccessViewModel.accessState.collectAsState()
        val adminContacts by userAccessViewModel.adminContacts.collectAsState()
        val currentUser = (accessState as? UserAccessState.Authorized)?.user
        LaunchedEffect(currentUser) { navigationViewModel.onUserChanged(currentUser) }
        if (currentUser == null) {
            // Wer schon angemeldet ist, aber (noch) keine Versammlung gewählt hat, wählt sie hier.
            if (selectedCongregation == null && accessState == UserAccessState.CongregationRequired) {
                CongregationRequiredScreen(onSignOut = signOut) {
                    CongregationSelector(selectionViewModel, selectionState)
                }
                return@AppTheme
            }
            UserAccessGateScreen(
                state = accessState,
                adminContacts = adminContacts,
                onRefresh = userAccessViewModel::refresh,
                onChangeCongregation = selectionViewModel::clearSelection,
                onSignOut = signOut,
            )
            return@AppTheme
        }

        val navigationState by navigationViewModel.uiState.collectAsState()
        AppScaffold(
            state = navigationState,
            firebaseStatus = firebaseStatus,
            currentUser = currentUser,
            actions = AppNavigationActions(
                onSelect = navigationViewModel::select,
                onOpenCongregationSection = navigationViewModel::openCongregationSection,
                onOpenGlobalSection = navigationViewModel::openGlobalSection,
                onSignOut = signOut,
                onBack = { navigationViewModel.back() },
            ),
        )
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
    onGlobalAdminLogin: (() -> Unit)? = null,
) {
    NightScreen(
        stepLabel = if (showCredentials) "SCHRITT 2 / 2" else "SCHRITT 1 / 2",
        current = if (showCredentials) congregationName ?: "Anmelden" else "Welche Versammlung?",
        previous = if (showCredentials) previousSteps else emptyList(),
    ) {
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
            onGlobalAdminLogin?.let { onClick ->
                SecondaryButton(
                    text = "Als globaler Admin anmelden",
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    outlined = false,
                )
            }
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

/** Schritt 2 für globale Admins: Anmeldung ohne Versammlung, mit dem Weg zurück zur Auswahl. */
@Composable
private fun GlobalLoginHint(onChooseCongregation: () -> Unit) {
    Text(
        text = "Globale Admins melden sich ohne Versammlung an.",
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        style = nightTextStyle(13.sp, color = NightColors.OnBackgroundMuted),
    )
    SecondaryButton(
        text = "Doch eine Versammlung wählen",
        onClick = onChooseCongregation,
        modifier = Modifier.fillMaxWidth(),
        outlined = false,
    )
    Spacer(Modifier.height(14.dp))
}

/** Für bereits angemeldete Nutzer, deren Versammlung noch nicht feststeht (z. B. nach einem Update). */
@Composable
private fun CongregationRequiredScreen(onSignOut: () -> Unit, selector: @Composable () -> Unit) {
    NightScreen(stepLabel = "ZUGANG", current = "Versammlung wählen") {
        PanelTitle("Versammlung wählen", "Wähle deine Versammlung, um die App zu nutzen.")
        Spacer(Modifier.height(14.dp))
        selector()
        Spacer(Modifier.weight(1f))
        SecondaryButton(text = "Abmelden", onClick = onSignOut, modifier = Modifier.fillMaxWidth(), outlined = false)
    }
}

private val previewRef = CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin-mitte")

private val pickingState = CongregationSelectionUiState(
    levels = listOf(
        OrgLevelSelection(OrgLevel.REGION, listOf(OrgUnit("EMEA")), selectedId = "EMEA"),
        OrgLevelSelection(OrgLevel.SUBREGION, listOf(OrgUnit("europe", "Europa")), selectedId = "europe"),
        OrgLevelSelection(OrgLevel.BRANCH, isLoading = true),
    ),
)

private val selectedState = CongregationSelectionUiState(
    selected = SelectedCongregation(previewRef, "Berlin-Mitte"),
)

@Composable
private fun LoginPreviewContent(
    selection: CongregationSelectionUiState,
    authMode: AuthMode = AuthMode.LOGIN,
    loginError: String? = null,
) = NightPhonePreview {
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
        showCredentials = selection.selected != null,
        congregationName = selection.selected?.displayName,
        previousSteps = listOf("Kreis 609a"),
        congregationSelector = {
            CongregationSelector(
                state = selection,
                onStartPicking = {},
                onCancelPicking = {},
                onSelectUnit = { _, _ -> },
                onInviteLinkChange = {},
                onApplyInviteLink = {},
                onPasteInviteLink = {},
                onRetry = {},
            )
        },
    )
}

@Preview(name = "Congregation Planer Login")
@Composable
fun AppPreview() = LoginPreviewContent(selectedState)

/** Schritt 1: Auswahl läuft. */
@ThemePreviews
@Composable
private fun LoginSelectionPreview() = LoginPreviewContent(pickingState)

/** Schritt 2: Versammlung gewählt, Anmeldung. */
@ThemePreviews
@Composable
private fun LoginCongregationSelectedPreview() = LoginPreviewContent(selectedState)

@ThemePreviews
@Composable
private fun LoginRegisterPreview() = LoginPreviewContent(selectedState, AuthMode.REGISTER)

@ThemePreviews
@Composable
private fun LoginErrorPreview() = LoginPreviewContent(selectedState, loginError = "E-Mail oder Passwort ist falsch.")
