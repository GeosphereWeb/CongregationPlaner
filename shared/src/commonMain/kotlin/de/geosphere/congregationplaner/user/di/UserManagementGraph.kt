package de.geosphere.congregationplaner.user.di

import de.geosphere.congregationplaner.talk.data.createExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.data.createTalkScheduleRepository
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.user.data.createUserRepository
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import de.geosphere.congregationplaner.user.presentation.management.UserManagementViewModel
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface UserManagementGraph {
    val userManagementViewModel: UserManagementViewModel

    @Provides
    fun provideUserRepository(): UserRepository = createUserRepository()

    // Redner übernehmen und Konten verknüpfen stellen Termine um und lesen das Rednerverzeichnis.
    @Provides
    fun provideTalkScheduleRepository(): TalkScheduleRepository = createTalkScheduleRepository()

    @Provides
    fun provideExternalSpeakerDirectoryRepository(): ExternalSpeakerDirectoryRepository =
        createExternalSpeakerDirectoryRepository()
}
