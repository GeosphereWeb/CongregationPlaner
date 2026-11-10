package de.geosphere.congregationplaner.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import de.geosphere.congregationplaner.organization.presentation.selection.CongregationSelectionViewModel
import de.geosphere.congregationplaner.organization.presentation.selection.CongregationSelector

/** Verbindet die Anmeldung mit [AuthViewModel] und der Versammlungsauswahl. */
@Composable
fun LoginRoute(viewModel: AuthViewModel, selectionViewModel: CongregationSelectionViewModel) {
    val state by viewModel.uiState.collectAsState()
    val selection by selectionViewModel.uiState.collectAsState()
    LoginContent(
        state = state,
        step = loginStep(selection, state.globalLogin),
        actions = LoginActions(
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onFirstNameChange = viewModel::onFirstNameChange,
            onLastNameChange = viewModel::onLastNameChange,
            onToggleMode = viewModel::toggleMode,
            onSubmit = viewModel::submit,
            onGoogleClick = viewModel::signInWithGoogle,
            onGlobalAdminLogin = viewModel::startGlobalLogin,
        ),
        congregationSelector = {
            if (state.globalLogin) {
                GlobalLoginHint(onChooseCongregation = viewModel::chooseCongregation)
            } else {
                CongregationSelector(selectionViewModel, selection)
            }
        },
    )
}
