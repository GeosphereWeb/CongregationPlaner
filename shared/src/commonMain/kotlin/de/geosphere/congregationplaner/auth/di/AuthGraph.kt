package de.geosphere.congregationplaner.auth.di

import de.geosphere.congregationplaner.auth.data.FirebaseAuthManager
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseAuthRepository
import de.geosphere.congregationplaner.auth.presentation.AuthViewModel
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface AuthGraph {
    val authViewModel: AuthViewModel

    @Provides
    fun provideAuthRepository(): FirebaseAuthRepository = FirebaseAuthManager
}
