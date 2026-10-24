package de.geosphere.congregationplaner.talk.domain.repository

import de.geosphere.congregationplaner.talk.domain.model.Talk
import kotlinx.coroutines.flow.Flow

interface TalkCatalogRepository {
    /** Die globale Vortragsliste; sie wird außerhalb der App (Firebase-Konsole) gepflegt. */
    fun observeTalks(): Flow<List<Talk>>
}
