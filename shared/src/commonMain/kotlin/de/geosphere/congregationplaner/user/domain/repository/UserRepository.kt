package de.geosphere.congregationplaner.user.domain.repository

import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.user.domain.model.AppUser
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /**
     * Liest den [AppUser] vom Server oder legt ihn an, falls er noch nicht existiert.
     * Neue Nutzer werden immer mit `approved = false` angelegt.
     */
    suspend fun getOrCreateUser(authUser: FirebaseUser): Result<AppUser>

    /** Beobachtet das Nutzerdokument; emittiert `null`, solange es nicht existiert. */
    fun observeUser(uid: String): Flow<AppUser?>
}
