package de.geosphere.congregationplaner.notice.data

import de.geosphere.congregationplaner.notice.domain.model.NoticeColorSet
import de.geosphere.congregationplaner.notice.domain.model.TalkNotice

/** Renders the notice as a PDF. The Android and desktop apps can; elsewhere [isAvailable] is `false`. */
interface NoticePdfRenderer {
    val isAvailable: Boolean

    /** [png] is the optional graphic; one that cannot be decoded is left out instead of failing the PDF. */
    suspend fun render(notice: TalkNotice, colors: NoticeColorSet, png: ByteArray?): Result<ByteArray>
}

/** Platform choice of the renderer, like the repository factories. */
expect fun createNoticePdfRenderer(): NoticePdfRenderer
