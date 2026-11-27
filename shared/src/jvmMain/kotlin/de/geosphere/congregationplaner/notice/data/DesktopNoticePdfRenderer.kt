package de.geosphere.congregationplaner.notice.data

import de.geosphere.congregationplaner.notice.domain.model.NoticeColorSet
import de.geosphere.congregationplaner.notice.domain.model.TalkNotice
import de.geosphere.congregationplaner.notice.pdf.RgbaImage
import de.geosphere.congregationplaner.notice.pdf.TalkNoticePdf
import de.geosphere.congregationplaner.notice.pdf.noticeImageSampleSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

actual fun createNoticePdfRenderer(): NoticePdfRenderer = DesktopNoticePdfRenderer

internal object DesktopNoticePdfRenderer : NoticePdfRenderer {
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
        readSubsampled(png)?.let { img ->
            val argb = img.getRGB(0, 0, img.width, img.height, null, 0, img.width)
            RgbaImage.fromArgb(img.width, img.height, argb)
        }
    } catch (_: Exception) {
        null
    } catch (_: OutOfMemoryError) {
        null
    }

    /** Reads the image with subsampling so a huge PNG is never held at full size. */
    private fun readSubsampled(png: ByteArray): BufferedImage? {
        val input = ImageIO.createImageInputStream(ByteArrayInputStream(png))
        val reader = ImageIO.getImageReaders(input).takeIf { it.hasNext() }?.next() ?: return null
        return try {
            reader.input = input
            val sample = noticeImageSampleSize(reader.getWidth(0), reader.getHeight(0))
            reader.read(0, reader.defaultReadParam.apply { setSourceSubsampling(sample, sample, 0, 0) })
        } finally {
            reader.dispose()
        }
    }
}
