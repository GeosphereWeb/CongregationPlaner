package de.geosphere.congregationplaner.auth.presentation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.model.SelectedCongregation
import de.geosphere.congregationplaner.organization.presentation.selection.CongregationSelectionUiState
import de.geosphere.congregationplaner.organization.presentation.selection.CongregationSelector
import de.geosphere.congregationplaner.organization.presentation.selection.OrgLevelSelection
import de.geosphere.congregationplaner.theming.NightColors
import de.geosphere.congregationplaner.theming.NightPhonePreview
import de.geosphere.congregationplaner.theming.ThemePreviews
import de.geosphere.congregationplaner.theming.nightTextStyle
import de.geosphere.congregationplaner.ui.button.SecondaryButton
import de.geosphere.congregationplaner.ui.header.NightScreen
import de.geosphere.congregationplaner.ui.header.PanelTitle

/**
 * Zweistufige Anmeldung im Nachtblau-Design: erst die Versammlung (über [congregationSelector]), danach die
 * Zugangsdaten. [step] legt fest, welcher Schritt gerade gilt.
 */
@Composable
fun LoginContent(
    state: LoginUiState,
    step: LoginStep,
    firebaseStatus: String,
    actions: LoginActions,
    congregationSelector: @Composable () -> Unit = {},
) {
    NightScreen(stepLabel = step.stepLabel, current = step.title, previous = step.previousSteps) {
        if (step.showCredentials) {
            congregationSelector()
            CredentialsForm(state, step.canSignIn, actions)
        } else {
            if (!step.isNewCongregationFlow) {
                PanelTitle("Versammlung wählen", "Wähle deine Versammlung, bevor du dich anmeldest.")
                Spacer(Modifier.height(14.dp))
            }
            congregationSelector()
            Spacer(Modifier.weight(1f))
            if (!step.isNewCongregationFlow) {
                SecondaryButton(
                    text = "Als globaler Admin anmelden",
                    onClick = actions.onGlobalAdminLogin,
                    modifier = Modifier.fillMaxWidth(),
                    outlined = false,
                )
                Text(
                    text = "Anmeldung folgt, sobald die Versammlung feststeht.",
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    textAlign = TextAlign.Center,
                    style = nightTextStyle(13.sp, color = NightColors.OnBackgroundMuted),
                )
            }
        }
        Text(
            text = firebaseStatus,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            textAlign = TextAlign.Center,
            style = nightTextStyle(11.sp, color = NightColors.OnBackgroundMuted, monospace = true),
        )
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
    selected = SelectedCongregation(previewRef, "Berlin-Mitte", "609a"),
)

@Composable
private fun LoginPreviewContent(selection: CongregationSelectionUiState, state: LoginUiState = LoginUiState()) =
    NightPhonePreview {
        LoginContent(
            state = state.copy(form = LoginForm(email = "name@beispiel.de", password = "geheimeswort")),
            step = loginStep(selection, globalLogin = false),
            firebaseStatus = "Firebase verfügbar",
            actions = LoginActions(),
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
private fun LoginRegisterPreview() = LoginPreviewContent(selectedState, LoginUiState(mode = AuthMode.REGISTER))

@ThemePreviews
@Composable
private fun LoginErrorPreview() =
    LoginPreviewContent(selectedState, LoginUiState(errorMessage = "E-Mail oder Passwort ist falsch."))
