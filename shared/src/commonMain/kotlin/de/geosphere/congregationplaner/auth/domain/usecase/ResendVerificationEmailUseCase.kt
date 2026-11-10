package de.geosphere.congregationplaner.auth.domain.usecase

import de.geosphere.congregationplaner.auth.domain.repository.FirebaseAuthRepository
import dev.zacsweers.metro.Inject

@Inject
class ResendVerificationEmailUseCase(private val repository: FirebaseAuthRepository) {
    suspend operator fun invoke(): Boolean = repository.resendVerificationEmail()
}
