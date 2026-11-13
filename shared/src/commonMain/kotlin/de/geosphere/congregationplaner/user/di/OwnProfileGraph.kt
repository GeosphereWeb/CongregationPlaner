package de.geosphere.congregationplaner.user.di

import de.geosphere.congregationplaner.user.data.createMessageTemplateRepository
import de.geosphere.congregationplaner.user.data.createUserRepository
import de.geosphere.congregationplaner.user.domain.repository.MessageTemplateRepository
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import de.geosphere.congregationplaner.user.presentation.profile.MessageTemplatesViewModel
import de.geosphere.congregationplaner.user.presentation.profile.OwnProfileViewModel
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface OwnProfileGraph {
    val ownProfileViewModel: OwnProfileViewModel
    val messageTemplatesViewModel: MessageTemplatesViewModel

    @Provides
    fun provideUserRepository(): UserRepository = createUserRepository()

    @Provides
    fun provideMessageTemplateRepository(): MessageTemplateRepository = createMessageTemplateRepository()
}
