package de.geosphere.congregationplaner.talk.data

import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.data.FirebaseAuthManager
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.firestoreFields
import de.geosphere.congregationplaner.firestore.firestoreNullableString
import de.geosphere.congregationplaner.firestore.listAllDocuments
import de.geosphere.congregationplaner.firestore.optionalStringField
import de.geosphere.congregationplaner.firestore.pollingFlow
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkPlanningSettings
import de.geosphere.congregationplaner.talk.domain.repository.TalkPlanningSettingsRepository
import kotlinx.coroutines.flow.Flow
import java.net.http.HttpClient

internal class FirestoreRestTalkPlanningSettingsRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : TalkPlanningSettingsRepository {
    private val client = FirestoreRestClient(httpClient, idTokenProvider, "Sign in before accessing the talk planning.")

    override fun observe(congregation: CongregationRef): Flow<TalkPlanningSettings> = pollingFlow {
        client.listAllDocuments(congregation.settingsPath)
            .firstOrNull { it.id == TALK_PLANNING_SETTINGS_ID }
            ?.let { document ->
                TalkPlanningSettings(
                    speakerFreeSince = parseSettingsDate(document.fields.optionalStringField(SPEAKER_FREE_SINCE_FIELD)),
                    talkFreeSince = parseSettingsDate(document.fields.optionalStringField(TALK_FREE_SINCE_FIELD)),
                )
            } ?: TalkPlanningSettings()
    }

    override suspend fun save(
        congregation: CongregationRef,
        settings: TalkPlanningSettings,
    ): Result<TalkPlanningSettings> = runCatchingIo {
        val body = firestoreFields(
            JsonObject().apply {
                add(SPEAKER_FREE_SINCE_FIELD, firestoreNullableString(settings.speakerFreeSince?.toString()))
                add(TALK_FREE_SINCE_FIELD, firestoreNullableString(settings.talkFreeSince?.toString()))
            },
        )
        // PATCH ohne updateMask ersetzt das Dokument und legt es bei Bedarf an.
        client.request("PATCH", client.documentUrl(congregation.settingsPath, TALK_PLANNING_SETTINGS_ID), body)
        settings
    }
}
