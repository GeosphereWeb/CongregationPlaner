package de.geosphere.congregationplaner.notice.di

import de.geosphere.congregationplaner.congregation.data.createCongregationMetadataRepository
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.notice.data.NoticeFileSource
import de.geosphere.congregationplaner.notice.data.NoticePdfRenderer
import de.geosphere.congregationplaner.notice.data.createNoticeFileSource
import de.geosphere.congregationplaner.notice.data.createNoticePdfRenderer
import de.geosphere.congregationplaner.notice.data.createTalkNoticeRepository
import de.geosphere.congregationplaner.notice.domain.repository.TalkNoticeRepository
import de.geosphere.congregationplaner.notice.presentation.TalkNoticeViewModel
import de.geosphere.congregationplaner.talk.data.createTalkScheduleRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface TalkNoticeGraph {
    val talkNoticeViewModel: TalkNoticeViewModel

    @Provides
    fun provideTalkNoticeRepository(): TalkNoticeRepository = createTalkNoticeRepository()

    @Provides
    fun provideNoticeFileSource(): NoticeFileSource = createNoticeFileSource()

    @Provides
    fun provideNoticePdfRenderer(): NoticePdfRenderer = createNoticePdfRenderer()

    @Provides
    fun provideTalkScheduleRepository(): TalkScheduleRepository = createTalkScheduleRepository()

    @Provides
    fun provideCongregationMetadataRepository(): CongregationMetadataRepository = createCongregationMetadataRepository()
}
