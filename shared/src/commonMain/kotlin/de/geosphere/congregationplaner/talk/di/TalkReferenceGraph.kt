package de.geosphere.congregationplaner.talk.di

import de.geosphere.congregationplaner.organization.data.createOrganizationRepository
import de.geosphere.congregationplaner.organization.domain.repository.OrganizationRepository
import de.geosphere.congregationplaner.talk.data.createTalkCatalogRepository
import de.geosphere.congregationplaner.talk.data.createWeekEventRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import de.geosphere.congregationplaner.talk.presentation.reference.TalkCatalogManagementViewModel
import de.geosphere.congregationplaner.talk.presentation.reference.WeekEventManagementViewModel
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface TalkReferenceGraph {
    val talkCatalogManagementViewModel: TalkCatalogManagementViewModel
    val weekEventManagementViewModel: WeekEventManagementViewModel

    @Provides
    fun provideTalkCatalogRepository(): TalkCatalogRepository = createTalkCatalogRepository()

    @Provides
    fun provideWeekEventRepository(): WeekEventRepository = createWeekEventRepository()

    @Provides
    fun provideOrganizationRepository(): OrganizationRepository = createOrganizationRepository()
}
