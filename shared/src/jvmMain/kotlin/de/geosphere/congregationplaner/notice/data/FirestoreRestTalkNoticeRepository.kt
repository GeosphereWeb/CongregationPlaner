package de.geosphere.congregationplaner.notice.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.geosphere.congregationplaner.auth.data.FirebaseAuthManager
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.firestoreFields
import de.geosphere.congregationplaner.firestore.firestoreNullableString
import de.geosphere.congregationplaner.firestore.firestoreString
import de.geosphere.congregationplaner.firestore.listAllDocuments
import de.geosphere.congregationplaner.firestore.optionalStringField
import de.geosphere.congregationplaner.firestore.parseDocument
import de.geosphere.congregationplaner.firestore.pollingFlow
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.firestore.stringField
import de.geosphere.congregationplaner.notice.domain.model.NoticeColorSet
import de.geosphere.congregationplaner.notice.domain.model.NoticeImage
import de.geosphere.congregationplaner.notice.domain.model.TalkNoticeSettings
import de.geosphere.congregationplaner.notice.domain.repository.TalkNoticeRepository
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.data.settingsPath
import kotlinx.coroutines.flow.Flow
import java.net.http.HttpClient
import java.util.Base64

private const val COUNT_ALIAS = "count"
private val COUNT_IMAGES_QUERY = """{"structuredAggregationQuery":{"structuredQuery":""" +
    """{"from":[{"collectionId":"$NOTICE_IMAGES_SUBCOLLECTION"}]},""" +
    """"aggregations":[{"alias":"$COUNT_ALIAS","count":{}}]}}"""

actual fun createTalkNoticeRepository(): TalkNoticeRepository = FirestoreRestTalkNoticeRepository()

internal class FirestoreRestTalkNoticeRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : TalkNoticeRepository {
    private val client = FirestoreRestClient(httpClient, idTokenProvider, "Sign in before accessing the talk notice.")

    override fun observeSettings(congregation: CongregationRef): Flow<TalkNoticeSettings> = pollingFlow {
        client.listAllDocuments(congregation.settingsPath)
            .firstOrNull { it.id == TALK_NOTICE_SETTINGS_ID }
            ?.let { document ->
                TalkNoticeSettings(
                    title = document.fields.optionalStringField(NOTICE_TITLE_FIELD) ?: TalkNoticeSettings().title,
                    colorSet = NoticeColorSet.fromKey(document.fields.optionalStringField(NOTICE_COLOR_SET_FIELD)),
                    defaultImageId = document.fields.optionalStringField(NOTICE_DEFAULT_IMAGE_FIELD),
                )
            } ?: TalkNoticeSettings()
    }

    override suspend fun saveSettings(
        congregation: CongregationRef,
        settings: TalkNoticeSettings,
    ): Result<TalkNoticeSettings> = runCatchingIo {
        val body = firestoreFields(
            JsonObject().apply {
                add(NOTICE_TITLE_FIELD, firestoreString(settings.title))
                add(NOTICE_COLOR_SET_FIELD, firestoreString(settings.colorSet.key))
                add(NOTICE_DEFAULT_IMAGE_FIELD, firestoreNullableString(settings.defaultImageId))
            },
        )
        // PATCH without updateMask replaces the document and creates it if needed.
        client.request("PATCH", client.documentUrl(congregation.settingsPath, TALK_NOTICE_SETTINGS_ID), body)
        settings
    }

    // A one-time read: the images are large and only needed when the notice section opens.
    override suspend fun loadImages(congregation: CongregationRef): Result<List<NoticeImage>> = runCatchingIo {
        client.listAllDocuments(congregation.noticeImagesPath).map { document ->
            NoticeImage(
                id = document.id,
                name = document.fields.stringField(NOTICE_IMAGE_NAME_FIELD),
                png = Base64.getDecoder().decode(
                    document.fields.getAsJsonObject(NOTICE_IMAGE_PNG_FIELD)?.get("bytesValue")?.asString
                        ?: throw IllegalArgumentException("Firestore field 'png' must be bytes."),
                ),
            )
        }
    }

    // Counted on the server (:runAggregationQuery) so the large image data is not transferred.
    override suspend fun countImages(congregation: CongregationRef): Result<Int> = runCatchingIo {
        val url = "${client.collectionUrl(congregation.documentPath)}:runAggregationQuery"
        JsonParser.parseString(client.request("POST", url, COUNT_IMAGES_QUERY))
            .asJsonArray.first().asJsonObject
            .getAsJsonObject("result").getAsJsonObject("aggregateFields").getAsJsonObject(COUNT_ALIAS)
            .get("integerValue").asString.toInt()
    }

    override suspend fun addImage(congregation: CongregationRef, name: String, png: ByteArray): Result<NoticeImage> =
        runCatchingIo {
            val body = firestoreFields(
                JsonObject().apply {
                    add(NOTICE_IMAGE_NAME_FIELD, firestoreString(name))
                    add(
                        NOTICE_IMAGE_PNG_FIELD,
                        JsonObject().apply { addProperty("bytesValue", Base64.getEncoder().encodeToString(png)) },
                    )
                },
            )
            val url = client.collectionUrl(congregation.noticeImagesPath)
            NoticeImage(parseDocument(client.request("POST", url, body)).id, name, png)
        }

    override suspend fun renameImage(congregation: CongregationRef, id: String, name: String): Result<Unit> =
        runCatchingIo {
            val url = "${client.documentUrl(congregation.noticeImagesPath, id)}?" +
                client.updateMask(listOf(NOTICE_IMAGE_NAME_FIELD))
            val body = firestoreFields(JsonObject().apply { add(NOTICE_IMAGE_NAME_FIELD, firestoreString(name)) })
            client.request("PATCH", url, body)
            Unit
        }

    override suspend fun deleteImage(congregation: CongregationRef, id: String): Result<Unit> = runCatchingIo {
        client.request("DELETE", client.documentUrl(congregation.noticeImagesPath, id))
        Unit
    }
}
