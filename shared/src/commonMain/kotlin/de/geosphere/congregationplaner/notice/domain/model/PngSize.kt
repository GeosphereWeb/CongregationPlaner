package de.geosphere.congregationplaner.notice.domain.model

private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
private val IHDR = byteArrayOf(0x49, 0x48, 0x44, 0x52)
private const val TYPE_OFFSET = 12
private const val WIDTH_OFFSET = 16
private const val HEIGHT_OFFSET = 20
private const val INT_BYTES = 4
private const val BYTE_BITS = 8
private const val BYTE_MASK = 0xFF
private const val IHDR_END = 24

/** Width and height from the IHDR chunk of a PNG; `null` if [png] does not start with a signature and IHDR. */
fun pngSize(png: ByteArray): Pair<Int, Int>? {
    if (png.size < IHDR_END) return null
    if (!png.copyOfRange(0, PNG_SIGNATURE.size).contentEquals(PNG_SIGNATURE)) return null
    if (!png.copyOfRange(TYPE_OFFSET, TYPE_OFFSET + IHDR.size).contentEquals(IHDR)) return null
    return png.bigEndianInt(WIDTH_OFFSET) to png.bigEndianInt(HEIGHT_OFFSET)
}

private fun ByteArray.bigEndianInt(offset: Int): Int =
    (0 until INT_BYTES).fold(0) { value, i -> (value shl BYTE_BITS) or (this[offset + i].toInt() and BYTE_MASK) }
