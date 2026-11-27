package de.geosphere.congregationplaner.notice.testing

/** The first bytes of a PNG: signature plus an IHDR chunk with the given size (the rest is not a real image). */
internal fun testPng(width: Int = 1, height: Int = 1, extraBytes: Int = 0): ByteArray {
    val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    val ihdr = byteArrayOf(0, 0, 0, 13, 0x49, 0x48, 0x44, 0x52)
    return signature + ihdr + width.toBigEndian() + height.toBigEndian() + ByteArray(extraBytes)
}

private fun Int.toBigEndian() = ByteArray(4) { i -> (this shr (24 - 8 * i)).toByte() }
