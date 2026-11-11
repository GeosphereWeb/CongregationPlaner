package de.geosphere.congregationplaner.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/** Zweiter Schritt der Anmeldung: Zugangsdaten, sobald die Versammlung feststeht. */
@Suppress("LongMethod", "MagicNumber")
@Composable
internal fun ColumnScope.CredentialsForm(state: LoginUiState, canSignIn: Boolean, actions: LoginActions) {
    val isLogin = state.isLogin
    Column(modifier = Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PanelTitle(
            title = if (isLogin) "Anmelden" else "Konto erstellen",
            subtitle = if (isLogin) "Weiter zu deiner Versammlung." else "Lege dein Konto für diese Versammlung an.",
        )
        CredentialFields(state, actions.onFormChange)
        state.errorMessage?.let { Text(it, style = nightTextStyle(14.sp, color = NightColors.Coral)) }
        state.infoMessage?.let { Text(it, style = nightTextStyle(14.sp, color = NightColors.Sky)) }
        PrimaryButton(
            text = if (isLogin) "Anmelden" else "Konto erstellen",
            onClick = actions.onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = canSignIn && state.hasRequiredNames && !state.isBusy,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = NightColors.Border)
            Text("oder", style = nightTextStyle(13.sp, FontWeight.Medium, NightColors.OnBackgroundMuted))
            HorizontalDivider(modifier = Modifier.weight(1f), color = NightColors.Border)
        }
        SecondaryButton(
            text = "Mit Google fortfahren",
            onClick = actions.onGoogleClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = canSignIn && !state.isBusy,
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
            onClick = actions.onToggleMode,
            outlined = false,
            contentColor = NightColors.OnBackground,
        )
    }
}

/** Eingabefelder der Anmeldung; beim Registrieren kommen Name und die Wiederholung von E-Mail und Passwort dazu. */
@Composable
private fun CredentialFields(state: LoginUiState, onFormChange: (LoginForm) -> Unit) {
    val form = state.form
    val isRegister = !state.isLogin
    var passwordVisible by remember { mutableStateOf(false) }
    if (isRegister) NameFields(form, onFormChange)
    val isError = state.errorMessage != null
    val emailKeyboard = KeyboardOptions(
        capitalization = KeyboardCapitalization.None,
        autoCorrectEnabled = false,
        keyboardType = KeyboardType.Email,
    )
    val passwordKeyboard = emailKeyboard.copy(keyboardType = KeyboardType.Password)
    val passwordTransformation =
        if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
    FieldBox(
        label = "E-Mail",
        value = form.email,
        onValueChange = { onFormChange(form.copy(email = it)) },
        placeholder = "name@beispiel.de",
        isError = isError,
        keyboardOptions = emailKeyboard,
    )
    if (isRegister) {
        FieldBox(
            label = "E-Mail wiederholen",
            value = form.emailConfirmation,
            onValueChange = { onFormChange(form.copy(emailConfirmation = it)) },
            placeholder = "name@beispiel.de",
            isError = isError,
            keyboardOptions = emailKeyboard,
        )
    }
    FieldBox(
        label = "Passwort",
        value = form.password,
        onValueChange = { onFormChange(form.copy(password = it)) },
        isError = isError,
        visualTransformation = passwordTransformation,
        trailing = { PasswordVisibilityButton(passwordVisible, onToggle = { passwordVisible = !passwordVisible }) },
        keyboardOptions = passwordKeyboard,
    )
    if (isRegister) {
        FieldBox(
            label = "Passwort wiederholen",
            value = form.passwordConfirmation,
            onValueChange = { onFormChange(form.copy(passwordConfirmation = it)) },
            isError = isError,
            visualTransformation = passwordTransformation,
            keyboardOptions = passwordKeyboard,
        )
    }
}

/** Vor- und Nachname, nur beim Registrieren. */
@Composable
private fun NameFields(form: LoginForm, onFormChange: (LoginForm) -> Unit) {
    FieldBox(
        label = "Vorname",
        value = form.firstName,
        onValueChange = { onFormChange(form.copy(firstName = it)) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    )
    FieldBox(
        label = "Nachname",
        value = form.lastName,
        onValueChange = { onFormChange(form.copy(lastName = it)) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    )
}

@ThemePreviews
@Composable
private fun CredentialsFormPreview() = NightPhonePreview {
    NightScreen(stepLabel = "SCHRITT 2 / 2", current = "Berlin-Mitte") {
        CredentialsForm(
            state = LoginUiState(
                mode = AuthMode.REGISTER,
                form = LoginForm(
                    email = "name@beispiel.de",
                    emailConfirmation = "name@beispiel.de",
                    password = "geheim",
                    passwordConfirmation = "geheimm",
                    firstName = "Max",
                ),
                errorMessage = "Die Passwörter stimmen nicht überein.",
            ),
            canSignIn = true,
            actions = LoginActions(),
        )
    }
}

@ThemePreviews
@Composable
private fun CredentialFieldsPreview() = NightPhonePreview {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CredentialFields(LoginUiState(form = LoginForm(email = "name@beispiel.de")), onFormChange = {})
        CredentialFields(
            LoginUiState(
                mode = AuthMode.REGISTER,
                form = LoginForm(email = "name@beispiel.de", emailConfirmation = "name@beispeil.de"),
                errorMessage = "Die E-Mail-Adressen stimmen nicht überein.",
            ),
            onFormChange = {},
        )
    }
}

@ThemePreviews
@Composable
private fun NameFieldsPreview() = NightPhonePreview {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        NameFields(LoginForm(firstName = "Max", lastName = "Muster"), onFormChange = {})
        NameFields(LoginForm(), onFormChange = {})
    }
}
