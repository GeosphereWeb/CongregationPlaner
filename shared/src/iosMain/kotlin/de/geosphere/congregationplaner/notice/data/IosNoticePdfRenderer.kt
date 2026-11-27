package de.geosphere.congregationplaner.notice.data

import de.geosphere.congregationplaner.notice.domain.model.NoticeColorSet
import de.geosphere.congregationplaner.notice.domain.model.TalkNotice

actual fun createNoticePdfRenderer(): NoticePdfRenderer = IosNoticePdfRenderer

internal object IosNoticePdfRenderer : NoticePdfRenderer {
    override val isAvailable: Boolean = false

    override suspend fun render(notice: TalkNotice, colors: NoticeColorSet, png: ByteArray?): Result<ByteArray> =
        Result.failure(UnsupportedOperationException("Den Aushang gibt es nur in der Android- und Desktop-App."))
}
