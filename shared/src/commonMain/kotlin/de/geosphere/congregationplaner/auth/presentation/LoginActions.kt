package de.geosphere.congregationplaner.auth.presentation

/** Callbacks der Anmeldung; gebündelt, damit die Composables überschaubare Signaturen behalten. */
data class LoginActions(
    val onFormChange: (LoginForm) -> Unit = {},
    val onToggleMode: () -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onGoogleClick: () -> Unit = {},
    val onGlobalAdminLogin: () -> Unit = {},
)
