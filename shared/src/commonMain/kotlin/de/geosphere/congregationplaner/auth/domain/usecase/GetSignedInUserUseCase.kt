package de.geosphere.congregationplaner.auth.domain.usecase

import de.geosphere.congregationplaner.auth.domain.repository.FirebaseAuthRepository
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseUser
import dev.zacsweers.metro.Inject

/** Der noch angemeldete Nutzer, z. B. nach einem Neustart; eine Sitzung ohne Nutzer wird beendet. */
@Inject
class GetSignedInUserUseCase(private val repository: FirebaseAuthRepository) {
    suspend operator fun invoke(): FirebaseUser? {
        if (!repository.isSignedIn()) return null
        return repository.currentUser() ?: run {
            repository.signOut()
            null
        }
    }
}
