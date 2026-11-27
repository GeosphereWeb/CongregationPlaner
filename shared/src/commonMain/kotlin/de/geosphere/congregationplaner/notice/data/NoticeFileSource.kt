package de.geosphere.congregationplaner.notice.data

/** A PNG chosen by the user; [name] is the file name without extension, cut to 60 characters. */
class PickedPng(val name: String, val bytes: ByteArray)

/**
 * Lets the user pick a PNG graphic and choose where to save the PDF. The Android and desktop apps can; elsewhere
 * [isAvailable] is `false`.
 */
interface NoticeFileSource {
    val isAvailable: Boolean

    /** Asks for a PNG file; `null` if the user cancelled. */
    suspend fun pickPng(): Result<PickedPng?>

    /** Asks where to save the PDF, proposing [fileName]; the chosen file name, `null` if the user cancelled. */
    suspend fun savePdf(fileName: String, bytes: ByteArray): Result<String?>
}

/** Platform choice of the file source, like the repository factories. */
expect fun createNoticeFileSource(): NoticeFileSource

internal const val MAX_PICKED_NAME_LENGTH = 60

/** The display name of a picked file: no extension, at most [MAX_PICKED_NAME_LENGTH] characters. */
internal fun pickedPngName(fileName: String): String = fileName.substringBeforeLast('.').take(MAX_PICKED_NAME_LENGTH)

internal object UnavailableNoticeFileSource : NoticeFileSource {
    override val isAvailable: Boolean = false

    override suspend fun pickPng(): Result<PickedPng?> = unavailable()

    override suspend fun savePdf(fileName: String, bytes: ByteArray): Result<String?> = unavailable()

    private fun <T> unavailable(): Result<T> =
        Result.failure(UnsupportedOperationException("Dateidialoge gibt es nur in der Android- und Desktop-App."))
}
