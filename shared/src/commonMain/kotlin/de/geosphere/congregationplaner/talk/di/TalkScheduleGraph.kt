package de.geosphere.congregationplaner.talk.di

import de.geosphere.congregationplaner.talk.data.createTalkScheduleRepository
import de.geosphere.congregationplaner.talk.data.createWeekEventRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.talk.domain.repository.WeekEventRepository
import de.geosphere.congregationplaner.talk.presentation.TalkScheduleViewModel
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface TalkScheduleGraph {
    val talkScheduleViewModel: TalkScheduleViewModel

    @Provides
    fun provideTalkScheduleRepository(): TalkScheduleRepository = createTalkScheduleRepository()

    @Provides
    fun provideWeekEventRepository(): WeekEventRepository = createWeekEventRepository()
}
