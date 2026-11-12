package de.geosphere.congregationplaner.talk.di

import de.geosphere.congregationplaner.talk.data.createTalkCatalogRepository
import de.geosphere.congregationplaner.talk.data.createTalkScheduleRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.talk.presentation.history.TalkHistoryViewModel
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface TalkHistoryGraph {
    val talkHistoryViewModel: TalkHistoryViewModel

    @Provides
    fun provideTalkCatalogRepository(): TalkCatalogRepository = createTalkCatalogRepository()

    // Die Historie wird aus den Terminen berechnet.
    @Provides
    fun provideTalkScheduleRepository(): TalkScheduleRepository = createTalkScheduleRepository()
}
