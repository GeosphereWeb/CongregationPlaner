package de.geosphere.congregationplaner.home.di

import de.geosphere.congregationplaner.home.presentation.HomeViewModel
import de.geosphere.congregationplaner.talk.data.createTalkScheduleRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.user.data.createUserRepository
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface HomeGraph {
    val homeViewModel: HomeViewModel

    @Provides
    fun provideTalkScheduleRepository(): TalkScheduleRepository = createTalkScheduleRepository()

    @Provides
    fun provideUserRepository(): UserRepository = createUserRepository()
}
