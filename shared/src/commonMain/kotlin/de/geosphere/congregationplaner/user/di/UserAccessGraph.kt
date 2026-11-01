package de.geosphere.congregationplaner.user.di

import de.geosphere.congregationplaner.user.data.createAdminContactRepository
import de.geosphere.congregationplaner.user.data.createUserRepository
import de.geosphere.congregationplaner.user.domain.repository.AdminContactRepository
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import de.geosphere.congregationplaner.user.presentation.UserAccessViewModel
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface UserAccessGraph {
    val userAccessViewModel: UserAccessViewModel

    @Provides
    fun provideUserRepository(): UserRepository = createUserRepository()

    @Provides
    fun provideAdminContactRepository(): AdminContactRepository = createAdminContactRepository()
}
