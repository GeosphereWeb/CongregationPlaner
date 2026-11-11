package de.geosphere.congregationplaner.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import de.geosphere.congregationplaner.organization.presentation.selection.CongregationSelectionViewModel
import de.geosphere.congregationplaner.organization.presentation.selection.CongregationSelector

/** Verbindet die Anmeldung mit [LoginViewModel] und der Versammlungsauswahl. */
@Composable
fun LoginRoute(viewModel: LoginViewModel, selectionViewModel: CongregationSelectionViewModel, firebaseStatus: String) {
    val state by viewModel.uiState.collectAsState()
    val selection by selectionViewModel.uiState.collectAsState()
    LoginContent(
        state = state,
        step = loginStep(selection, state.globalLogin),
        firebaseStatus = firebaseStatus,
        actions = LoginActions(
            onFormChange = viewModel::onFormChange,
            onToggleMode = viewModel::toggleMode,
            onSubmit = viewModel::submit,
            onGoogleClick = viewModel::signInWithGoogle,
            onGlobalAdminLogin = { viewModel.setGlobalLogin(true) },
        ),
        congregationSelector = {
            if (state.globalLogin) {
                GlobalLoginHint(onChooseCongregation = { viewModel.setGlobalLogin(false) })
            } else {
                CongregationSelector(selectionViewModel, selection)
            }
        },
    )
}
