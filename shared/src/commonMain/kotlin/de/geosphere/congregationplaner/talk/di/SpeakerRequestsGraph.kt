package de.geosphere.congregationplaner.talk.di

import de.geosphere.congregationplaner.talk.data.createExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.data.createSpeakerRequestRepository
import de.geosphere.congregationplaner.talk.data.createTalkCatalogRepository
import de.geosphere.congregationplaner.talk.data.createTalkScheduleRepository
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.domain.repository.SpeakerRequestRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.talk.presentation.requests.SpeakerRequestsViewModel
import de.geosphere.congregationplaner.user.data.createUserRepository
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface SpeakerRequestsGraph {
    val speakerRequestsViewModel: SpeakerRequestsViewModel

    @Provides
    fun provideSpeakerRequestRepository(): SpeakerRequestRepository = createSpeakerRequestRepository()

    // Termine mit offener Anfrage und freie Tage zum Zuweisen.
    @Provides
    fun provideTalkScheduleRepository(): TalkScheduleRepository = createTalkScheduleRepository()

    // Redner und Vorträge für eine Anfrage ohne Tag.
    @Provides
    fun provideUserRepository(): UserRepository = createUserRepository()

    @Provides
    fun provideExternalSpeakerDirectoryRepository(): ExternalSpeakerDirectoryRepository =
        createExternalSpeakerDirectoryRepository()

    @Provides
    fun provideTalkCatalogRepository(): TalkCatalogRepository = createTalkCatalogRepository()
}
