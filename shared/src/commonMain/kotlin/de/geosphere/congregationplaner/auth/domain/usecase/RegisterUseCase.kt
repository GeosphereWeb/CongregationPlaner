package de.geosphere.congregationplaner.auth.domain.usecase

import de.geosphere.congregationplaner.auth.domain.model.AuthException
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseAuthRepository
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseUser
import dev.zacsweers.metro.Inject

/** Legt ein Konto an; der Name („Vorname Nachname“) wird im Auth-Profil gespeichert. */
@Inject
class RegisterUseCase(private val repository: FirebaseAuthRepository) {
    suspend operator fun invoke(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
    ): Result<FirebaseUser> {
        if (firstName.isBlank() || lastName.isBlank()) return Result.failure(AuthException.MissingName())
        val displayName = "${firstName.trim()} ${lastName.trim()}"
        return repository.createUserWithEmailAndPassword(email.trim(), password, displayName)
            ?.let { Result.success(it) }
            ?: Result.failure(AuthException.RegistrationFailed())
    }
}
