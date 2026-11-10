package de.geosphere.congregationplaner.auth.domain.usecase

import de.geosphere.congregationplaner.auth.domain.model.AuthException
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseAuthRepository
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseUser
import dev.zacsweers.metro.Inject

@Inject
class SignInUseCase(private val repository: FirebaseAuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<FirebaseUser> =
        repository.signInWithEmailAndPassword(email.trim(), password)
            ?.let { Result.success(it) }
            ?: Result.failure(AuthException.SignInFailed())
}
