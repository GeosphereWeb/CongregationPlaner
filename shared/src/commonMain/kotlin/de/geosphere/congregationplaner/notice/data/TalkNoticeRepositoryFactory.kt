package de.geosphere.congregationplaner.notice.data

import de.geosphere.congregationplaner.notice.domain.model.NoticeImage
import de.geosphere.congregationplaner.notice.domain.model.TalkNoticeSettings
import de.geosphere.congregationplaner.notice.domain.repository.TalkNoticeRepository
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

expect fun createTalkNoticeRepository(): TalkNoticeRepository

/** Id of the notice settings document inside the congregation's `settings` collection. */
internal const val TALK_NOTICE_SETTINGS_ID = "talkNotice"
internal const val NOTICE_IMAGES_SUBCOLLECTION = "noticeImages"
internal const val NOTICE_TITLE_FIELD = "title"
internal const val NOTICE_COLOR_SET_FIELD = "colorSet"
internal const val NOTICE_DEFAULT_IMAGE_FIELD = "defaultImageId"
internal const val NOTICE_IMAGE_NAME_FIELD = "name"
internal const val NOTICE_IMAGE_PNG_FIELD = "png"

internal val CongregationRef.noticeImagesPath: String get() = "$documentPath/$NOTICE_IMAGES_SUBCOLLECTION"

fun unavailableTalkNoticeRepository(platform: String): TalkNoticeRepository = object : TalkNoticeRepository {
    private fun <T> unavailable(): Result<T> =
        Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

    override fun observeSettings(congregation: CongregationRef): Flow<TalkNoticeSettings> = flow {
        throw UnsupportedOperationException("Firestore is not configured for $platform.")
    }

    override suspend fun saveSettings(
        congregation: CongregationRef,
        settings: TalkNoticeSettings,
    ): Result<TalkNoticeSettings> = unavailable()

    override suspend fun loadImages(congregation: CongregationRef): Result<List<NoticeImage>> = unavailable()

    override suspend fun addImage(congregation: CongregationRef, name: String, png: ByteArray): Result<NoticeImage> =
        unavailable()

    override suspend fun renameImage(congregation: CongregationRef, id: String, name: String): Result<Unit> =
        unavailable()

    override suspend fun deleteImage(congregation: CongregationRef, id: String): Result<Unit> = unavailable()
}
