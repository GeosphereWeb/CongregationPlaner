package de.geosphere.congregationplaner.auth.domain.usecase

import de.geosphere.congregationplaner.auth.domain.model.AuthException
import de.geosphere.congregationplaner.auth.domain.model.Registration
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseAuthRepository
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseUser
import dev.zacsweers.metro.Inject

/**
 * Legt ein Konto an, wenn E-Mail und Passwort mit ihrer Wiederholung übereinstimmen; der Name („Vorname Nachname“)
 * wird im Auth-Profil gespeichert.
 */
@Inject
class RegisterUseCase(private val repository: FirebaseAuthRepository) {
    suspend operator fun invoke(registration: Registration): Result<FirebaseUser> = with(registration) {
        val email = email.trim()
        when {
            firstName.isBlank() || lastName.isBlank() -> return Result.failure(AuthException.MissingName())
            // E-Mail-Adressen unterscheiden in Firebase Auth nicht nach Groß-/Kleinschreibung.
            !email.equals(emailConfirmation.trim(), ignoreCase = true) ->
                return Result.failure(AuthException.EmailMismatch())
            password != passwordConfirmation -> return Result.failure(AuthException.PasswordMismatch())
        }
        val displayName = "${firstName.trim()} ${lastName.trim()}"
        repository.createUserWithEmailAndPassword(email, password, displayName)
            ?.let { Result.success(it) }
            ?: Result.failure(AuthException.RegistrationFailed())
    }
}
