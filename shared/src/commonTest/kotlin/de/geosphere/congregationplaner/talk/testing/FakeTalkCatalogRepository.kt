package de.geosphere.congregationplaner.talk.testing

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.repository.TalkCatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeTalkCatalogRepository(talks: List<Talk> = emptyList()) : TalkCatalogRepository {
    val talks = MutableStateFlow(talks)

    override fun observeTalks(): Flow<List<Talk>> = talks
}
