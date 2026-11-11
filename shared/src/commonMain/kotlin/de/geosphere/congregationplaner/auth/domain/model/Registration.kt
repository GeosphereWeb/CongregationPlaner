package de.geosphere.congregationplaner.auth.domain.model

/** Eingaben einer Registrierung; E-Mail und Passwort werden zur Kontrolle doppelt eingegeben. */
data class Registration(
    val email: String,
    val emailConfirmation: String,
    val password: String,
    val passwordConfirmation: String,
    val firstName: String,
    val lastName: String,
)
