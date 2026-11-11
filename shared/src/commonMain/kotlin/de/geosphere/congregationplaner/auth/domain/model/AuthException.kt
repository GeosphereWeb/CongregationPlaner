package de.geosphere.congregationplaner.auth.domain.model

/** Fehler bei Anmeldung und Registrierung; die Nachrichten werden direkt in der UI angezeigt. */
sealed class AuthException(message: String) : Exception(message) {
    class SignInFailed : AuthException("Login fehlgeschlagen. Bitte E-Mail und Passwort prüfen.")

    class RegistrationFailed : AuthException("Registrierung fehlgeschlagen. Bitte prüfe deine Eingaben.")

    class MissingName : AuthException("Bitte gib Vor- und Nachnamen ein.")

    class EmailMismatch : AuthException("Die E-Mail-Adressen stimmen nicht überein.")

    class PasswordMismatch : AuthException("Die Passwörter stimmen nicht überein.")

    class GoogleSignInFailed : AuthException("Google-Anmeldung fehlgeschlagen oder wurde abgebrochen.")
}
