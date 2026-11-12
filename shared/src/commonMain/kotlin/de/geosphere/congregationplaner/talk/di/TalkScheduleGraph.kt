package de.geosphere.congregationplaner.talk.di

import de.geosphere.congregationplaner.congregation.data.createCongregationMetadataRepository
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.talk.data.createExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.data.createTalkCatalogRepository
import de.geosphere.congregationplaner.talk.data.createTalkScheduleRepository
import de.geosphere.congregationplaner.talk.data.createWeekEventRepository
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import de.geosphere.congregationplaner.talk.presentation.schedule.TalkScheduleViewModel
import de.geosphere.congregationplaner.user.data.createUserRepository
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface TalkScheduleGraph {
    val talkScheduleViewModel: TalkScheduleViewModel

    @Provides
    fun provideTalkScheduleRepository(): TalkScheduleRepository = createTalkScheduleRepository()

    @Provides
    fun provideWeekEventRepository(): WeekEventRepository = createWeekEventRepository()

    @Provides
    fun provideTalkCatalogRepository(): TalkCatalogRepository = createTalkCatalogRepository()

    @Provides
    fun provideExternalSpeakerDirectoryRepository(): ExternalSpeakerDirectoryRepository =
        createExternalSpeakerDirectoryRepository()

    // Wochentag und Uhrzeit der Zusammenkunft am Wochenende für „Jahr anlegen“.
    @Provides
    fun provideCongregationMetadataRepository(): CongregationMetadataRepository = createCongregationMetadataRepository()

    // Die Redner stammen aus den Mitgliedern der Versammlung.
    @Provides
    fun provideUserRepository(): UserRepository = createUserRepository()
}
