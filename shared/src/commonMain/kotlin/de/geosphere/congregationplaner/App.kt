package de.geosphere.congregationplaner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import de.geosphere.congregationplaner.auth.data.FirebaseSupport
import de.geosphere.congregationplaner.auth.di.AuthGraph
import de.geosphere.congregationplaner.auth.presentation.LoginRoute
import de.geosphere.congregationplaner.home.di.HomeGraph
import de.geosphere.congregationplaner.organization.di.OrganizationGraph
import de.geosphere.congregationplaner.organization.presentation.selection.CongregationRequiredScreen
import de.geosphere.congregationplaner.organization.presentation.selection.CongregationSelector
import de.geosphere.congregationplaner.organization.presentation.selection.InviteLinkInbox
import de.geosphere.congregationplaner.organization.presentation.selection.NewCongregationScreen
import de.geosphere.congregationplaner.presentation.navigation.AppNavigationActions
import de.geosphere.congregationplaner.presentation.navigation.AppNavigationGraph
import de.geosphere.congregationplaner.presentation.navigation.AppScaffold
import de.geosphere.congregationplaner.theming.AppTheme
import de.geosphere.congregationplaner.user.di.UserAccessGraph
import de.geosphere.congregationplaner.user.domain.model.UserAccessState
import de.geosphere.congregationplaner.user.presentation.access.UserAccessGateScreen
import dev.zacsweers.metro.createGraph

/** Wurzel der App: Anmeldung, Freigabe und danach die Bereiche, je nach Sitzung und Nutzerstatus. */
@Suppress("LongMethod")
@Composable
fun App() {
    AppTheme {
        val authGraph = remember { createGraph<AuthGraph>() }
        val sessionViewModel = remember { authGraph.sessionViewModel }
        val loginViewModel = remember { authGraph.loginViewModel }
        val userAccessViewModel = remember { createGraph<UserAccessGraph>().userAccessViewModel }
        val selectionViewModel = remember { createGraph<OrganizationGraph>().congregationSelectionViewModel }
        val navigationViewModel = remember { createGraph<AppNavigationGraph>().appNavigationViewModel }
        val homeViewModel = remember { createGraph<HomeGraph>().homeViewModel }
        val authState by sessionViewModel.uiState.collectAsState()
        val loginState by loginViewModel.uiState.collectAsState()
        val selectionState by selectionViewModel.uiState.collectAsState()
        val selectedCongregation = selectionState.activeSelection?.ref
        val newCongregation = selectionState.newCongregation
        val pendingInviteLink by InviteLinkInbox.pendingLink.collectAsState()
        val authUser = authState.user

        LaunchedEffect(Unit) {
            FirebaseSupport.initialize()
            sessionViewModel.onFirebaseInitialized(FirebaseSupport.isReady())
        }

        // Eine geglückte Anmeldung übernimmt die Sitzung; die Anmeldung beginnt danach wieder von vorn.
        LaunchedEffect(loginState.signedInUser) {
            val user = loginState.signedInUser ?: return@LaunchedEffect
            sessionViewModel.onSignedIn(user)
            loginViewModel.onSignInHandled()
        }

        // Ein Einladungslink, mit dem die App gestartet wurde, wählt die Versammlung vor.
        LaunchedEffect(pendingInviteLink) {
            val link = pendingInviteLink ?: return@LaunchedEffect
            selectionViewModel.inviteLink.applyLink(link)
            InviteLinkInbox.consume()
        }

        // Die Nutzerdaten liegen unterhalb der Versammlung; ohne gewählte Versammlung gibt es keinen Zugriff, außer für
        // globale Admins (ADR-0013).
        LaunchedEffect(authUser, selectedCongregation) {
            if (authUser == null) {
                userAccessViewModel.onSignedOut()
            } else {
                userAccessViewModel.onSignedIn(authUser, selectedCongregation)
            }
        }

        if (authUser == null) {
            LoginRoute(loginViewModel, selectionViewModel, authState.firebaseStatus)
            return@AppTheme
        }

        val signOut: () -> Unit = {
            sessionViewModel.signOut()
            // The effects below do not run on the login screen, so reset here: the next sign-in starts at home.
            navigationViewModel.onUserChanged(null)
        }

        // Erst nach erteilter Freigabe ist die App nutzbar; Rollen steuern die sichtbaren Bereiche.
        val accessState by userAccessViewModel.accessState.collectAsState()
        val adminContacts by userAccessViewModel.adminContacts.collectAsState()
        val currentUser = (accessState as? UserAccessState.Authorized)?.user
        LaunchedEffect(currentUser) { navigationViewModel.onUserChanged(currentUser) }
        LaunchedEffect(currentUser) { homeViewModel.onCurrentUserChanged(currentUser) }
        if (currentUser == null) {
            // Wer eine neue Versammlung vorgemerkt hat, legt sie an, sobald die E-Mail-Adresse bestätigt ist.
            if (selectedCongregation == null &&
                newCongregation != null &&
                accessState == UserAccessState.CongregationRequired
            ) {
                NewCongregationScreen(
                    draft = newCongregation,
                    emailVerified = authState.emailVerified,
                    isCreating = selectionState.isCreating,
                    errorMessage = selectionState.errorMessage,
                    onCreate = { selectionViewModel.newCongregation.create(authUser) },
                    onRefresh = sessionViewModel::refreshEmailVerification,
                    onResend = sessionViewModel::resendVerificationEmail,
                    onChangeName = selectionViewModel.newCongregation::cancel,
                    onSignOut = signOut,
                )
                return@AppTheme
            }
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
        val homeState by homeViewModel.uiState.collectAsState()
        AppScaffold(
            state = navigationState,
            firebaseStatus = authState.firebaseStatus,
            currentUser = currentUser,
            congregation = selectionState.selected,
            home = homeState,
            actions = AppNavigationActions(
                onSelect = navigationViewModel::select,
                onOpenCongregationSection = navigationViewModel::openCongregationSection,
                onOpenGlobalSection = navigationViewModel::openGlobalSection,
                onOpenTalkHistory = navigationViewModel::openTalkHistory,
                onSignOut = signOut,
                onBack = { navigationViewModel.back() },
            ),
        )
    }
}
