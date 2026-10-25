package de.geosphere.congregationplaner.talk.di

import de.geosphere.congregationplaner.talk.data.createExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.presentation.ExternalDirectoryViewModel
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface ExternalDirectoryGraph {
    val externalDirectoryViewModel: ExternalDirectoryViewModel

    @Provides
    fun provideExternalSpeakerDirectoryRepository(): ExternalSpeakerDirectoryRepository =
        createExternalSpeakerDirectoryRepository()
}
