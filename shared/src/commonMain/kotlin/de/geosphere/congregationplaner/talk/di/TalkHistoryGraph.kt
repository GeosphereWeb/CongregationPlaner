package de.geosphere.congregationplaner.talk.di

import de.geosphere.congregationplaner.talk.data.createTalkCatalogRepository
import de.geosphere.congregationplaner.talk.data.createTalkHistoryRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import de.geosphere.congregationplaner.talk.domain.repository.TalkHistoryRepository
import de.geosphere.congregationplaner.talk.presentation.TalkHistoryViewModel
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface TalkHistoryGraph {
    val talkHistoryViewModel: TalkHistoryViewModel

    @Provides
    fun provideTalkCatalogRepository(): TalkCatalogRepository = createTalkCatalogRepository()

    @Provides
    fun provideTalkHistoryRepository(): TalkHistoryRepository = createTalkHistoryRepository()
}
