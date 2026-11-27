package de.geosphere.congregationplaner.notice.domain.repository

import de.geosphere.congregationplaner.notice.domain.model.NoticeImage
import de.geosphere.congregationplaner.notice.domain.model.TalkNoticeSettings
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import kotlinx.coroutines.flow.Flow

interface TalkNoticeRepository {
    fun observeSettings(congregation: CongregationRef): Flow<TalkNoticeSettings>

    suspend fun saveSettings(congregation: CongregationRef, settings: TalkNoticeSettings): Result<TalkNoticeSettings>

    suspend fun loadImages(congregation: CongregationRef): Result<List<NoticeImage>>

    /** The number of images, without reading their data. */
    suspend fun countImages(congregation: CongregationRef): Result<Int>

    suspend fun addImage(congregation: CongregationRef, name: String, png: ByteArray): Result<NoticeImage>

    suspend fun renameImage(congregation: CongregationRef, id: String, name: String): Result<Unit>

    suspend fun deleteImage(congregation: CongregationRef, id: String): Result<Unit>
}
