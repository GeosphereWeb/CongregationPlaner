package de.geosphere.congregationplaner.auth.domain.usecase

import de.geosphere.congregationplaner.auth.domain.model.AuthException
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseAuthRepository
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseUser
import dev.zacsweers.metro.Inject

@Inject
class SignInWithGoogleUseCase(private val repository: FirebaseAuthRepository) {
    suspend operator fun invoke(): Result<FirebaseUser> =
        repository.signInWithGoogle()?.let { Result.success(it) } ?: Result.failure(AuthException.GoogleSignInFailed())
}
