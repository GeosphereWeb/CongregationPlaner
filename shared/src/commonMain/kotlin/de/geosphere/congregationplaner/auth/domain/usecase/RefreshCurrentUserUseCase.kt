package de.geosphere.congregationplaner.auth.domain.usecase

import de.geosphere.congregationplaner.auth.domain.repository.FirebaseAuthRepository
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseUser
import dev.zacsweers.metro.Inject

/** Liest das Konto neu, z. B. nachdem die E-Mail-Adresse bestätigt wurde. */
@Inject
class RefreshCurrentUserUseCase(private val repository: FirebaseAuthRepository) {
    suspend operator fun invoke(): FirebaseUser? = repository.refreshCurrentUser()
}
