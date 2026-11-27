package de.geosphere.congregationplaner.notice.data

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.data.AndroidFirestoreProvider
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.observeDocuments
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.firestore.stringField
import de.geosphere.congregationplaner.notice.domain.model.NoticeColorSet
import de.geosphere.congregationplaner.notice.domain.model.NoticeImage
import de.geosphere.congregationplaner.notice.domain.model.TalkNoticeSettings
import de.geosphere.congregationplaner.notice.domain.repository.TalkNoticeRepository
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.data.settingsPath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

actual fun createTalkNoticeRepository(): TalkNoticeRepository =
    FirestoreTalkNoticeRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreTalkNoticeRepository(private val firestore: FirebaseFirestore) : TalkNoticeRepository {
    // The settings collection only holds settings documents, so the collection listener is enough.
    override fun observeSettings(congregation: CongregationRef): Flow<TalkNoticeSettings> =
        observeDocuments(firestore.collection(congregation.settingsPath), "setting") { document ->
            document.id to TalkNoticeSettings(
                title = document.getString(NOTICE_TITLE_FIELD) ?: TalkNoticeSettings().title,
                colorSet = NoticeColorSet.fromKey(document.getString(NOTICE_COLOR_SET_FIELD)),
                defaultImageId = document.getString(NOTICE_DEFAULT_IMAGE_FIELD),
            )
        }.map { documents ->
            documents.firstOrNull { it.first == TALK_NOTICE_SETTINGS_ID }?.second ?: TalkNoticeSettings()
        }

    override suspend fun saveSettings(
        congregation: CongregationRef,
        settings: TalkNoticeSettings,
    ): Result<TalkNoticeSettings> = runFirestoreCatching("save talk notice settings") {
        firestore.collection(congregation.settingsPath).document(TALK_NOTICE_SETTINGS_ID)
            .set(
                mapOf(
                    NOTICE_TITLE_FIELD to settings.title,
                    NOTICE_COLOR_SET_FIELD to settings.colorSet.key,
                    NOTICE_DEFAULT_IMAGE_FIELD to settings.defaultImageId,
                ),
            ).awaitResult()
        settings
    }

    // A one-time read: the images are large and only needed when the notice section opens.
    override suspend fun loadImages(congregation: CongregationRef): Result<List<NoticeImage>> =
        runFirestoreCatching("load notice images") {
            firestore.collection(congregation.noticeImagesPath).get().awaitResult().documents.map { document ->
                NoticeImage(
                    id = document.id,
                    name = document.stringField(NOTICE_IMAGE_NAME_FIELD),
                    png = document.getBlob(NOTICE_IMAGE_PNG_FIELD)?.toBytes()
                        ?: throw IllegalArgumentException("Firestore field 'png' must be bytes."),
                )
            }
        }

    override suspend fun countImages(congregation: CongregationRef): Result<Int> =
        runFirestoreCatching("count notice images") {
            firestore.collection(congregation.noticeImagesPath).count().get(AggregateSource.SERVER).awaitResult()
                .count.toInt()
        }

    override suspend fun addImage(congregation: CongregationRef, name: String, png: ByteArray): Result<NoticeImage> =
        runFirestoreCatching("add notice image") {
            val reference = firestore.collection(congregation.noticeImagesPath)
                .add(mapOf(NOTICE_IMAGE_NAME_FIELD to name, NOTICE_IMAGE_PNG_FIELD to Blob.fromBytes(png)))
                .awaitResult()
            NoticeImage(reference.id, name, png)
        }

    override suspend fun renameImage(congregation: CongregationRef, id: String, name: String): Result<Unit> =
        runFirestoreCatching("rename notice image") {
            firestore.collection(congregation.noticeImagesPath).document(id)
                .update(NOTICE_IMAGE_NAME_FIELD, name).awaitResult()
            Unit
        }

    override suspend fun deleteImage(congregation: CongregationRef, id: String): Result<Unit> =
        runFirestoreCatching("delete notice image") {
            firestore.collection(congregation.noticeImagesPath).document(id).delete().awaitResult()
            Unit
        }
}
