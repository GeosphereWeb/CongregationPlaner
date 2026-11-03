package de.geosphere.congregationplaner.user.di

import de.geosphere.congregationplaner.user.data.createMemberPrivateRepository
import de.geosphere.congregationplaner.user.data.createUserRepository
import de.geosphere.congregationplaner.user.domain.repository.MemberPrivateRepository
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import de.geosphere.congregationplaner.user.presentation.UserManagementViewModel
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface UserManagementGraph {
    val userManagementViewModel: UserManagementViewModel

    @Provides
    fun provideUserRepository(): UserRepository = createUserRepository()

    @Provides
    fun provideMemberPrivateRepository(): MemberPrivateRepository = createMemberPrivateRepository()
}
