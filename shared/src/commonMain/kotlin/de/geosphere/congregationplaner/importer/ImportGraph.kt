package de.geosphere.congregationplaner.importer

import de.geosphere.congregationplaner.congregation.data.createCongregationMetadataRepository
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.talk.data.createExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.data.createTalkCatalogRepository
import de.geosphere.congregationplaner.talk.data.createTalkHistoryRepository
import de.geosphere.congregationplaner.talk.data.createTalkScheduleRepository
import de.geosphere.congregationplaner.talk.data.createWeekEventRepository
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkHistoryRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface ImportGraph {
    val importViewModel: ImportViewModel

    @Provides
    fun provideImportFileSource(): ImportFileSource = createImportFileSource()

    @Provides
    fun provideWeekEventRepository(): WeekEventRepository = createWeekEventRepository()

    @Provides
    fun provideTalkCatalogRepository(): TalkCatalogRepository = createTalkCatalogRepository()

    @Provides
    fun provideTalkScheduleRepository(): TalkScheduleRepository = createTalkScheduleRepository()

    @Provides
    fun provideExternalSpeakerDirectoryRepository(): ExternalSpeakerDirectoryRepository =
        createExternalSpeakerDirectoryRepository()

    @Provides
    fun provideTalkHistoryRepository(): TalkHistoryRepository = createTalkHistoryRepository()

    @Provides
    fun provideCongregationMetadataRepository(): CongregationMetadataRepository = createCongregationMetadataRepository()
}
