package de.geosphere.congregationplaner.talk.data

import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.data.AndroidFirestoreProvider
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.observeDocuments
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkPlanningSettings
import de.geosphere.congregationplaner.talk.domain.repository.TalkPlanningSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

actual fun createTalkPlanningSettingsRepository(): TalkPlanningSettingsRepository =
    FirestoreTalkPlanningSettingsRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreTalkPlanningSettingsRepository(private val firestore: FirebaseFirestore) :
    TalkPlanningSettingsRepository {
    // Die Collection hält nur Einstellungsdokumente; so genügt der vorhandene Collection-Listener.
    override fun observe(congregation: CongregationRef): Flow<TalkPlanningSettings> =
        observeDocuments(firestore.collection(congregation.settingsPath), "setting") { document ->
            document.id to TalkPlanningSettings(
                speakerFreeSince = parseSettingsDate(document.getString(SPEAKER_FREE_SINCE_FIELD)),
                talkFreeSince = parseSettingsDate(document.getString(TALK_FREE_SINCE_FIELD)),
            )
        }.map { documents ->
            documents.firstOrNull { it.first == TALK_PLANNING_SETTINGS_ID }?.second ?: TalkPlanningSettings()
        }

    override suspend fun save(
        congregation: CongregationRef,
        settings: TalkPlanningSettings,
    ): Result<TalkPlanningSettings> = runFirestoreCatching("save talk planning settings") {
        firestore.collection(congregation.settingsPath).document(TALK_PLANNING_SETTINGS_ID)
            .set(
                mapOf(
                    SPEAKER_FREE_SINCE_FIELD to settings.speakerFreeSince?.toString(),
                    TALK_FREE_SINCE_FIELD to settings.talkFreeSince?.toString(),
                ),
            ).awaitResult()
        settings
    }
}
