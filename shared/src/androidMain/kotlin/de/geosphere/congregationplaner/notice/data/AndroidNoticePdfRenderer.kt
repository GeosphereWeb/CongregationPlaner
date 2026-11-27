package de.geosphere.congregationplaner.notice.data

import android.graphics.BitmapFactory
import de.geosphere.congregationplaner.notice.domain.model.NoticeColorSet
import de.geosphere.congregationplaner.notice.domain.model.TalkNotice
import de.geosphere.congregationplaner.notice.pdf.RgbaImage
import de.geosphere.congregationplaner.notice.pdf.TalkNoticePdf
import de.geosphere.congregationplaner.notice.pdf.noticeImageSampleSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual fun createNoticePdfRenderer(): NoticePdfRenderer = AndroidNoticePdfRenderer

internal object AndroidNoticePdfRenderer : NoticePdfRenderer {
    override val isAvailable: Boolean = true

    @Suppress("TooGenericExceptionCaught") // any failure becomes a Result; cancellation is rethrown
    override suspend fun render(notice: TalkNotice, colors: NoticeColorSet, png: ByteArray?): Result<ByteArray> =
        withContext(Dispatchers.Default) {
            try {
                Result.success(TalkNoticePdf.layout(notice, colors, png?.let(::decode)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    @Suppress("TooGenericExceptionCaught") // an undecodable or too large graphic is simply left out
    private fun decode(png: ByteArray): RgbaImage? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(png, 0, png.size, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = noticeImageSampleSize(bounds.outWidth, bounds.outHeight)
        }
        BitmapFactory.decodeByteArray(png, 0, png.size, options)?.let { bmp ->
            val argb = IntArray(bmp.width * bmp.height)
            bmp.getPixels(argb, 0, bmp.width, 0, 0, bmp.width, bmp.height)
            RgbaImage.fromArgb(bmp.width, bmp.height, argb)
        }
    } catch (_: Exception) {
        null
    } catch (_: OutOfMemoryError) {
        null
    }
}
